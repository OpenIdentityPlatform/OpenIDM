({
    appDir: "../../../target/classes",
    baseUrl: "js",
    dir: "../../../target/minified",
    modules: [
        {   
            name: "main",
            excludeShallow: [
                "libs/mustache",
                "config/AppConfiguration",
                "config/process/AdminConfig",
                "config/process/CommonConfig",
                "config/process/UserConfig"
            ]
        }
    ]
})