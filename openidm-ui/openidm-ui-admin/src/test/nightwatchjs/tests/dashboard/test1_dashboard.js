module.exports = {
    'Load dashboard': function (client) {
        //must login first at the beginning of a session
        //client must already have frame widget added
        /*
         {
             "type" : "frame",
             "size" : "small",
             "frameUrl" : "http://localhost:5601/#/visualize/create?embed&type=area&indexPattern=ba*&_g=()&_a=(filters:!(),linked:!f,query:(query_string:(analyze_wildcard:!t,query:'*')),vis:(aggs:!((id:'1',params:(),schema:metric,type:count),(id:'2',params:(extended_bounds:(),field:account_number,interval:500),schema:segment,type:histogram)),listeners:(),params:(addLegend:!t,addTimeMarker:!f,addTooltip:!t,defaultYExtents:!f,interpolate:linear,mode:stacked,scale:linear,setYExtents:!f,shareYAxis:!t,smoothLines:!f,times:!(),yAxis:()),type:area))",
             "title" : "Balance Pie",
             "height" : "550px",
             "width" : "100%"
         }
         */
        client.globals.login.helpers.login(client);

        client.waitForElementPresent('#dashboardWidgets', 2000)
            .assert.elementPresent(".widget-holder");
    },

    'Frame widget present': function (client) {
        //Check frame widget displays and basic properties from ui config work
        client.assert.elementPresent(".widget-holder iframe");
        client.assert.cssProperty(".widget-holder iframe", "height", "550px");
        client.expect.element(".widget-holder .widget-section-title").text.to.equal("BALANCE PIE");

        client.end();
    }
};