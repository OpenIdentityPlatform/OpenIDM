/*
  backgrid-filter
  http://github.com/wyuenho/backgrid

  Copyright (c) 2013 Jimmy Yuen Ho Wong and contributors
  Licensed under the MIT @license.
*/
!function(a,b){if("function"==typeof define&&define.amd)
// AMD. Register as an anonymous module.
define(["underscore","backbone","backgrid"],b);else if("object"==typeof exports){
// CommonJS
var c;try{c=require("lunr")}catch(d){}module.exports=b(require("underscore"),require("backbone"),require("backgrid"),c)}else
// Browser
b(a._,a.Backbone,a.Backgrid,a.lunr)}(this,function(a,b,c,d){"use strict";/**
     ServerSideFilter is a search form widget that submits a query to the server
     for filtering the current collection.

     @class Backgrid.Extension.ServerSideFilter
  */
var e=c.Extension.ServerSideFilter=b.View.extend({/** @property */
tagName:"form",/** @property */
className:"backgrid-filter form-search",/** @property {function(Object, ?Object=): string} template */
template:function(a){return'<span class="search">&nbsp;</span><input type="search" '+(a.placeholder?'placeholder="'+a.placeholder+'"':"")+' name="'+a.name+'" '+(a.value?'value="'+a.value+'"':"")+'/><a class="clear" data-backgrid-action="clear" href="#">&times;</a>'},/** @property */
events:{"keyup input[type=search]":"showClearButtonMaybe","click a[data-backgrid-action=clear]":"clear",submit:"search"},/** @property {string} [name='q'] Query key */
name:"q",/** @property {string} [value] The search box value.  */
value:null,/**
       @property {string} [placeholder] The HTML5 placeholder to appear beneath
       the search box.
    */
placeholder:null,/**
       @param {Object} options
       @param {Backbone.Collection} options.collection
       @param {string} [options.name]
       @param {string} [options.value]
       @param {string} [options.placeholder]
       @param {function(Object): string} [options.template]
    */
initialize:function(a){e.__super__.initialize.apply(this,arguments),this.name=a.name||this.name,this.value=a.value||this.value,this.placeholder=a.placeholder||this.placeholder,this.template=a.template||this.template;
// Persist the query on pagination
var c=this.collection,d=this;b.PageableCollection&&c instanceof b.PageableCollection&&"server"==c.mode&&(c.queryParams[this.name]=function(){return d.query()||null})},/**
       Event handler. Clear the search box and reset the internal search value.
     */
clearSearchBox:function(){this.value=null,this.searchBox().val(null),this.showClearButtonMaybe()},/**
       Event handler. Show the clear button when the search box has text, hide
       it otherwise.
     */
showClearButtonMaybe:function(){var a=this.clearButton(),b=this.query();b?a.show():a.hide()},/**
       Returns the search input box.
     */
searchBox:function(){return this.$el.find("input[type=search]")},/**
       Returns the clear button.
     */
clearButton:function(){return this.$el.find("a[data-backgrid-action=clear]")},/**
       Returns the current search query.
     */
query:function(){return this.value=this.searchBox().val(),this.value},/**
       Upon search form submission, this event handler constructs a query
       parameter object and pass it to Collection#fetch for server-side
       filtering.

       If the collection is a PageableCollection, searching will go back to the
       first page.
    */
search:function(a){a&&a.preventDefault();var c={},d=this.query();d&&(c[this.name]=d);var e=this.collection;
// go back to the first page on search
b.PageableCollection&&e instanceof b.PageableCollection?e.getFirstPage({data:c,reset:!0,fetch:!0}):e.fetch({data:c,reset:!0})},/**
       Event handler for the clear button. Clears the search box and refetch the
       collection.

       If the collection is a PageableCollection, clearing will go back to the
       first page.
    */
clear:function(a){a&&a.preventDefault(),this.clearSearchBox();var c=this.collection;
// go back to the first page on clear
b.PageableCollection&&c instanceof b.PageableCollection?c.getFirstPage({reset:!0,fetch:!0}):c.fetch({reset:!0})},/**
       Renders a search form with a text box, optionally with a placeholder and
       a preset value if supplied during initialization.
    */
render:function(){return this.$el.empty().append(this.template({name:this.name,placeholder:this.placeholder,value:this.value})),this.showClearButtonMaybe(),this.delegateEvents(),this}}),f=c.Extension.ClientSideFilter=e.extend({/** @property */
events:a.extend({},e.prototype.events,{"click a[data-backgrid-action=clear]":function(a){a.preventDefault(),this.clear()},"keydown input[type=search]":"search",submit:function(a){a.preventDefault(),this.search()}}),/**
       @property {?Array.<string>} [fields] A list of model field names to
       search for matches. If null, all of the fields will be searched.
    */
fields:null,/**
       @property [wait=149] The time in milliseconds to wait since the last
       change to the search box's value before searching. This value can be
       adjusted depending on how often the search box is used and how large the
       search index is.
    */
wait:149,/**
       Debounces the #search and #clear methods and makes a copy of the given
       collection for searching.

       @param {Object} options
       @param {Backbone.Collection} options.collection
       @param {string} [options.placeholder]
       @param {string} [options.fields]
       @param {string} [options.wait=149]
    */
initialize:function(b){f.__super__.initialize.apply(this,arguments),this.fields=b.fields||this.fields,this.wait=b.wait||this.wait,this._debounceMethods(["search","clear"]);var c=this.collection=this.collection.fullCollection||this.collection,d=this.shadowCollection=c.clone();this.listenTo(c,"add",function(a,b,c){d.add(a,c)}),this.listenTo(c,"remove",function(a,b,c){d.remove(a,c)}),this.listenTo(c,"sort",function(a){this.query()||d.reset(a.models)}),this.listenTo(c,"reset",function(b,c){c=a.extend({reindex:!0},c||{}),c.reindex&&null==c.from&&null==c.to&&d.reset(b.models)})},_debounceMethods:function(b){a.isString(b)&&(b=[b]),this.undelegateEvents();for(var c=0,d=b.length;d>c;c++){var e=b[c],f=this[e];this[e]=a.debounce(f,this.wait)}this.delegateEvents()},/**
       Constructs a Javascript regular expression object for #makeMatcher.

       This default implementation takes a query string and returns a Javascript
       RegExp object that matches any of the words contained in the query string
       case-insensitively. Override this method to return a different regular
       expression matcher if this behavior is not desired.

       @param {string} query The search query in the search box.
       @return {RegExp} A RegExp object to match against model #fields.
     */
makeRegExp:function(a){return new RegExp(a.trim().split(/\s+/).join("|"),"i")},/**
       This default implementation takes a query string and returns a matcher
       function that looks for matches in the model's #fields or all of its
       fields if #fields is null, for any of the words in the query
       case-insensitively using the regular expression object returned from
       #makeRegExp.

       Most of time, you'd want to override the regular expression used for
       matching. If so, please refer to the #makeRegExp documentation,
       otherwise, you can override this method to return a custom matching
       function.

       Subclasses overriding this method must take care to conform to the
       signature of the matcher function. The matcher function is a function
       that takes a model as paramter and returns true if the model matches a
       search, or false otherwise.

       In addition, when the matcher function is called, its context will be
       bound to this ClientSideFilter object so it has access to the filter's
       attributes and methods.

       @param {string} query The search query in the search box.
       @return {function(Backbone.Model):boolean} A matching function.
    */
makeMatcher:function(a){var b=this.makeRegExp(a);return function(a){for(var c=this.fields||a.keys(),d=0,e=c.length;e>d;d++)if(b.test(a.get(c[d])+""))return!0;return!1}},/**
       Takes the query from the search box, constructs a matcher with it and
       loops through collection looking for matches. Reset the given collection
       when all the matches have been found.

       If the collection is a PageableCollection, searching will go back to the
       first page.
    */
search:function(){var b=a.bind(this.makeMatcher(this.query()),this),c=this.collection;c.pageableCollection&&c.pageableCollection.getFirstPage({silent:!0}),c.reset(this.shadowCollection.filter(b),{reindex:!1})},/**
       Clears the search box and reset the collection to its original.

       If the collection is a PageableCollection, clearing will go back to the
       first page.
    */
clear:function(){this.clearSearchBox();var a=this.collection;a.pageableCollection&&a.pageableCollection.getFirstPage({silent:!0}),a.reset(this.shadowCollection.models,{reindex:!1})}}),g=c.Extension.LunrFilter=f.extend({/**
       @property {string} [ref="id"]｀lunrjs` document reference attribute name.
    */
ref:"id",/**
       @property {Object} fields A hash of `lunrjs` index field names and boost
       value. Unlike ClientSideFilter#fields, LunrFilter#fields is _required_ to
       initialize the index.
    */
fields:null,/**
       Indexes the underlying collection on construction. The index will refresh
       when the underlying collection is reset. If any model is added, removed
       or if any indexed fields of any models has changed, the index will be
       updated.

       @param {Object} options
       @param {Backbone.Collection} options.collection
       @param {string} [options.placeholder]
       @param {string} [options.ref] ｀lunrjs` document reference attribute name.
       @param {Object} [options.fields] A hash of `lunrjs` index field names and
       boost value.
       @param {number} [options.wait]
    */
initialize:function(a){g.__super__.initialize.apply(this,arguments),this.ref=a.ref||this.ref;var b=this.collection=this.collection.fullCollection||this.collection;this.listenTo(b,"add",this.addToIndex),this.listenTo(b,"remove",this.removeFromIndex),this.listenTo(b,"reset",this.resetIndex),this.listenTo(b,"change",this.updateIndex),this.resetIndex(b)},/**
       Reindex the collection. If `options.reindex` is `false`, this method is a
       no-op.

       @param {Backbone.Collection} collection
       @param {Object} [options]
       @param {boolean} [options.reindex=true]
    */
resetIndex:function(b,c){if(c=a.extend({reindex:!0},c||{}),c.reindex){var e=this;this.index=d(function(){a.each(e.fields,function(a,b){this.field(b,a),this.ref(e.ref)},this)}),b.each(function(a){this.addToIndex(a)},this)}},/**
       Adds the given model to the index.

       @param {Backbone.Model} model
    */
addToIndex:function(a){var b=this.index,c=a.toJSON();b.documentStore.has(c[this.ref])?b.update(c):b.add(c)},/**
       Removes the given model from the index.

       @param {Backbone.Model} model
    */
removeFromIndex:function(a){var b=this.index,c=a.toJSON();b.documentStore.has(c[this.ref])&&b.remove(c)},/**
       Updates the index for the given model.

       @param {Backbone.Model} model
    */
updateIndex:function(b){var c=b.changedAttributes();c&&!a.isEmpty(a.intersection(a.keys(this.fields),a.keys(c)))&&this.index.update(b.toJSON())},/**
       Takes the query from the search box and performs a full-text search on
       the client-side. The search result is returned by resetting the
       underlying collection to the models after interrogating the index for the
       query answer.

       If the collection is a PageableCollection, searching will go back to the
       first page.
    */
search:function(){var a=this.collection;if(!this.query())return void a.reset(this.shadowCollection.models,{reindex:!1});for(var b=this.index.search(this.query()),c=[],d=0;d<b.length;d++){var e=b[d];c.push(this.shadowCollection.get(e.ref))}a.pageableCollection&&a.pageableCollection.getFirstPage({silent:!0}),a.reset(c,{reindex:!1})}})});