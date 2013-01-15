
/*global source */

(function () {
        
    var photosList = source.photos,
        photos = [],
        i = 0,
        getType = function (photoUrl) {
            var test = photoUrl.split("/").reverse()[0];
            switch (test) {
            case "F":
                return "photo";
            case "T":
                return "thumbnail";
            default:
                return "no-type";
            }
        };
    
    for (i = 0; i < photosList.length; i += 1) {
        photos.push({value: photosList[i], type : getType(photosList[i])});
    }
    return photos;

}());