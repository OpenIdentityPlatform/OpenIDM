// Copyright 2011 Mark Cavage, Inc.  All rights reserved.
define(function (require) {
var module = {};

//var asn1 = require('asn1');

var Protocol = require('./protocol');

var Filter = require('./filter');
var AndFilter = require('./and_filter');
var ApproximateFilter = require('./approx_filter');
var EqualityFilter = require('./equality_filter');
var ExtensibleFilter = require('./ext_filter');
var GreaterThanEqualsFilter = require('./ge_filter');
var LessThanEqualsFilter = require('./le_filter');
var NotFilter = require('./not_filter');
var OrFilter = require('./or_filter');
var PresenceFilter = require('./presence_filter');
var SubstringFilter = require('./substr_filter');


///--- Globals

//var BerReader = asn1.BerReader;



///--- Internal Parsers

// expression parsing
// returns the index of the closing parenthesis matching the open paren
// specified by openParenIndex
function matchParens(str, openParenIndex) {
  var stack = [];
  var esc = false;
  for (var i = openParenIndex || 0; i < str.length; i++) {
    var c = str[i];

    if (c === '\\') {
      if (!esc)
        esc = true;
      continue;
    } else if (c === '(' && !esc) {
      stack.push(1);
    } else if (c === ')' && !esc) {
      stack.pop();
      if (stack.length === 0)
        return i;
    }

    esc = false;
  }

  var ndx = str.length - 1;
  if (str.charAt(ndx) !== ')')
    throw new Error(str + ' has unbalanced parentheses');

  return ndx;
}


// recursive function that builds a filter tree from a string expression
// the filter tree is an intermediary step between the incoming expression and
// the outgoing Filter Class structure.
function _buildFilterTree(expr) {
  var c;
  var child;
  var clean = false;
  var endParen;
  var esc = false;
  var i = 0;
  var tree = {};
  var split;
  var substrNdx = 0;
  var val = '';

  if (expr.length === 0)
    return tree;

  // Chop the parens (the call to matchParens below gets rid of the trailer)
  if (expr.charAt(0) == '(')
    expr = expr.substring(1, expr.length - 1);

  //store prefix operator
  if (expr.charAt(0) === '&') {
    tree.op = 'and';
    expr = expr.substring(1);
  } else if (expr.charAt(0) === '|') {
    tree.op = 'or';
    expr = expr.substring(1);
  } else if (expr.charAt(0) === '!') {
    tree.op = 'not';
    expr = expr.substring(1);
  } else {
    tree.op = 'expr';
  }

  if (tree.op != 'expr') {
    tree.children = [];

    // logical operators are k-ary, so we go until our expression string runs
    // out (at least for this recursion level)
    while (expr.length !== 0) {
      endParen = matchParens(expr);

      if (endParen == expr.length - 1) {
        tree.children[i] = _buildFilterTree(expr);
        expr = '';
      } else {
        child = expr.slice(0, endParen + 1);
        expr = expr.substring(endParen + 1);
        tree.children[i] = _buildFilterTree(child);
      }
      i++;
    }
  } else {
    //else its some sort of non-logical expression, parse and return as such
    var operatorStr = '';
    tree.name = '';
    tree.value = '';


    // This parses and enforces filter syntax, which is an AttributeDescription
    // plus a filter operator, followed by (for ldapjs), anything.  Note
    // that ldapjs additionally allows the '_' character in the AD, as many
    // users rely on it, even though it's non-standard
    //
    // From 4.1.5 of RFC251
    //
    //  AttributeDescription ::= LDAPString
    //
    //  A value of AttributeDescription is based on the following BNF:
    //
    //    <AttributeDescription> ::= <AttributeType> [ ";" <options> ]
    //
    //    <options>  ::= <option> | <option> ";" <options>
    //
    //    <option>   ::= <opt-char> <opt-char>*
    //
    //    <opt-char> ::=  ASCII-equivalent letters, numbers and hyphen
    //
    // Examples of valid AttributeDescription:
    //
    //    cn
    //    userCertificate;binary


    if (expr.indexOf('~=') !== -1) {
      operatorStr = '~=';
      tree.tag = 'approxMatch';
    } else if (expr.indexOf('>=') !== -1) {
      operatorStr = '>=';
      tree.tag = 'greaterOrEqual';
    } else if (expr.indexOf('<=') !== -1) {
      operatorStr = '<=';
      tree.tag = 'lessOrEqual';
    } else if (expr.indexOf(':=') !== -1) {
      operatorStr = ':=';
      tree.tag = 'extensibleMatch';
    } else if (expr.indexOf('=') !== -1) {
      operatorStr = '=';
      tree.tag = 'equalityMatch';
    } else {
      // tree.tag = 'present';
      throw new Error('invalid filter syntax');
    }

    if (operatorStr === '') {
      tree.name = expr;
    } else {
      // pull out lhs and rhs of equality operator
      var splitAry = expr.split(operatorStr);
      tree.name = splitAry.shift();
      tree.value = splitAry.join(operatorStr);

      if (tree.tag == 'extensibleMatch') {
        split = tree.name.split(':');
        tree.extensible = {
          matchType: split[0],
          value: tree.value
        };
        switch (split.length) {
        case 1:
          break;
        case 2:
          if (split[1].toLowerCase() === 'dn') {
            tree.extensible.dnAttributes = true;
          } else {
            tree.extensible.rule = split[1];
          }
          break;
        case 3:
          tree.extensible.dnAttributes = true;
          tree.extensible.rule = split[2];
          break;
        default:
          throw new Error('Invalid extensible filter');
        }
      }
    }

    // Cleanup any escape sequences
    if (!clean) {

      for (i = 0; i < tree.value.length; i++) {
        c = tree.value[i];
        if (esc) {
          val += c;
          esc = false;
        } else if (c === '\\') {
          esc = true;
        } else {
          val += c;
        }
      }
      tree.value = val;
    }
  }

  return tree;
}


function serializeTree(tree, filter) {
  if (tree === undefined || tree.length === 0)
    return;

  // if the current tree object is not an expression then its a logical
  // operator (ie an internal node in the tree)
  var current = null;
  if (tree.op !== 'expr') {
    switch (tree.op) {
    case 'and':
      current = new AndFilter();
      break;
    case 'or':
      current = new OrFilter();
      break;
    case 'not':
      current = new NotFilter();
      break;
    default:
      break;
    }

    filter.addFilter(current || filter);
    if (current || tree.children.length) {
      tree.children.forEach(function (child) {
        serializeTree(child, current);
      });
    }
  } else {
    // else its a leaf node in the tree, and represents some type of
    // non-logical expression
    var tmp;

    // convert the tag name to a filter class type
    switch (tree.tag) {
    case 'approxMatch':
      tmp = new ApproximateFilter({
        attribute: tree.name,
        value: tree.value
      });
      break;
    case 'extensibleMatch':
      tmp = new ExtensibleFilter(tree.extensible);
      break;
    case 'greaterOrEqual':
      tmp = new GreaterThanEqualsFilter({
        attribute: tree.name,
        value: tree.value
      });
      break;
    case 'lessOrEqual':
      tmp = new LessThanEqualsFilter({
        attribute: tree.name,
        value: tree.value
      });
      break;
    case 'equalityMatch':
      tmp = new EqualityFilter({
        attribute: tree.name,
        value: tree.value
      });
      break;
    case 'substrings':
      tmp = new SubstringFilter({
        attribute: tree.name,
        initial: tree.initial,
        any: tree.any,
        'final': tree['final']
      });
      break;
    case 'present':
      tmp = new PresenceFilter({
        attribute: tree.name
      });
      break;
    default:
      break;
    }
    if (tmp)
      filter.addFilter(tmp);
  }
}


function _parseString(str) {

  // create a blank object to pass into treeToObjs
  // since its recursive we have to prime it ourselves.
  // this gets stripped off before the filter structure is returned
  // at the bottom of this function.
  var filterObj = new AndFilter({
    filters: []
  });

  serializeTree(_buildFilterTree(str), filterObj);
  return filterObj.filters[0];
}


/*
 * A filter looks like this coming in:
 *      Filter ::= CHOICE {
 *              and             [0]     SET OF Filter,
 *              or              [1]     SET OF Filter,
 *              not             [2]     Filter,
 *              equalityMatch   [3]     AttributeValueAssertion,
 *              substrings      [4]     SubstringFilter,
 *              greaterOrEqual  [5]     AttributeValueAssertion,
 *              lessOrEqual     [6]     AttributeValueAssertion,
 *              present         [7]     AttributeType,
 *              approxMatch     [8]     AttributeValueAssertion,
 *              extensibleMatch [9]     MatchingRuleAssertion --v3 only
 *      }
 *
 *      SubstringFilter ::= SEQUENCE {
 *              type               AttributeType,
 *              SEQUENCE OF CHOICE {
 *                      initial          [0] IA5String,
 *                      any              [1] IA5String,
 *                      final            [2] IA5String
 *              }
 *      }
 *
 * The extensibleMatch was added in LDAPv3:
 *
 *      MatchingRuleAssertion ::= SEQUENCE {
 *              matchingRule    [1] MatchingRuleID OPTIONAL,
 *              type            [2] AttributeDescription OPTIONAL,
 *              matchValue      [3] AssertionValue,
 *              dnAttributes    [4] BOOLEAN DEFAULT FALSE
 *      }
 */
function _parse(ber) {

  function parseSet(f) {
    var end = ber.offset + ber.length;
    while (ber.offset < end)
      f.addFilter(_parse(ber));
  }

  var f;

  var type = ber.readSequence();
  switch (type) {

  case Protocol.FILTER_AND:
    f = new AndFilter();
    parseSet(f);
    break;

  case Protocol.FILTER_APPROX:
    f = new ApproximateFilter();
    f.parse(ber);
    break;

  case Protocol.FILTER_EQUALITY:
    f = new EqualityFilter();
    f.parse(ber);
    return f;

  case Protocol.FILTER_EXT:
    f = new ExtensibleFilter();
    f.parse(ber);
    return f;

  case Protocol.FILTER_GE:
    f = new GreaterThanEqualsFilter();
    f.parse(ber);
    return f;

  case Protocol.FILTER_LE:
    f = new LessThanEqualsFilter();
    f.parse(ber);
    return f;

  case Protocol.FILTER_NOT:
    var _f = _parse(ber);
    f = new NotFilter({
      filter: _f
    });
    break;

  case Protocol.FILTER_OR:
    f = new OrFilter();
    parseSet(f);
    break;

  case Protocol.FILTER_PRESENT:
    f = new PresenceFilter();
    f.parse(ber);
    break;

  case Protocol.FILTER_SUBSTRINGS:
    f = new SubstringFilter();
    f.parse(ber);
    break;

  default:
    throw new Error('Invalid search filter type: 0x' + type.toString(16));
  }


  return f;
}



///--- API


return {
/*
  parse: function (ber) {
    if (!ber || !(ber instanceof BerReader))
      throw new TypeError('ber (BerReader) required');

    return _parse(ber);
  },
  parseString: function (filter) {
    if (!filter || typeof (filter) !== 'string')
      throw new TypeError('filter (string) required');

    return _parseString(filter);
  },
  AndFilter: AndFilter,
  ApproximateFilter: ApproximateFilter,
  EqualityFilter: EqualityFilter,
  ExtensibleFilter: ExtensibleFilter,
  GreaterThanEqualsFilter: GreaterThanEqualsFilter,
  LessThanEqualsFilter: LessThanEqualsFilter,
  NotFilter: NotFilter,
  OrFilter: OrFilter,
  PresenceFilter: PresenceFilter,
  SubstringFilter: SubstringFilter,
  Filter: Filter,

*/

  buildFilterTree: _buildFilterTree,
  
  serializeFilterTree: function (tree) {
    var filterObj = new AndFilter({
      filters: []
    });

    serializeTree(tree, filterObj);
    return filterObj.filters[0].toString();
  }
};

});