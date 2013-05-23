/* 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

managerId = source.manager.substring(4, source.indexof(','));
manager = openidm.read('/managed/user/'+managerId)

managerMap = {
    "managerId" : managerId,
    "$ref" : "/managed/user/"+managerId,
    "displayName" : manager.displayName
}

managerMap