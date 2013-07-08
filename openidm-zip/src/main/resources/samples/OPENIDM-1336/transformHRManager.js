/* 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

if (source !== null) {
    managerId = source.substring(4, source.indexOf(','));

    managerMap = {
        "managerId" : managerId,
        "$ref" : "/managed/user/"+managerId,
        "displayName" : managerId
    }
    managerMap
}

