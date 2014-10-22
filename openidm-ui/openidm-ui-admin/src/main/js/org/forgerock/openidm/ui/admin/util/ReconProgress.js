/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2014 ForgeRock AS. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

/*global $, define, _ */

/**
 * @author huck.elliott
 */
define("org/forgerock/openidm/ui/admin/util/ReconProgress", [],
  function() {

    var obj = {};
    
    obj.init = function(container, id,endTxt,startTxt){
        var progress = $(id, container),
            progressObj = {};
        
        progress.show();

        $(".progress-label", progress).text((startTxt) ? $.t(startTxt) : $.t("common.task.starting"));
        
        $(".reconProgressContainer").show();
        
        if(progress.length){
            progress.progressbar({
                value: false,
                change: function () {
                    $(".progress-label", progress).text(parseInt((progress.progressbar("option", "value") / progress.progressbar("option", "max"))*100, 10) + "%");
                }
            });
        }
        
        progressObj.start = function (reconStatus) {
            if(progress.length){
                if (reconStatus.progress.source.existing.total !== "?" &&
                    reconStatus.progress.target.existing.total !== "?") {
                    progress.progressbar("option", "max", parseInt(reconStatus.progress.source.existing.total, 10) + parseInt(reconStatus.progress.target.existing.total, 10));
                }
                
                progress.progressbar("option", "value", parseInt(reconStatus.progress.source.existing.processed, 10) + parseInt(reconStatus.progress.target.existing.processed, 10));
            }
        };
        
        progressObj.end = function(){
            if(progress.length){
                // force the max to be the current value, so that the bar completes no matter what
                progress.progressbar("option", "max", progress.progressbar("option", "value"));
                $(".progress-label", progress).text($.t(endTxt));
            }
        };
        
        return progressObj;
    };

    return obj;
});
