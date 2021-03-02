$( document ).ready(function(){
    $('.ui-diagram > .ui-diagram-element').contextmenu(function(info){
        // console.log('Node: ' + info.target.id);
        // console.log('Parent node: ' + info.target.parentNode.id);
        // console.log('Parent parent node: ' + info.target.parentNode.parentNode.id);

        var id;
        if (info.target.id.includes('objectId')) {
            id = info.target.id.substring(info.target.id.indexOf('objectId') + 8);
        } else {
            if (info.target.parentNode.id.includes('objectId')) {
                id = info.target.parentNode.id.substring(info.target.parentNode.id.indexOf('objectId') + 8);
            } else {
                id = info.target.parentNode.parentNode.id.substring(info.target.parentNode.parentNode.id.indexOf('objectId') + 8);
            }
        }
        if (id.includes('Right')) {
            id = id.replace('Right', '');
        }

        setObjectIDJSF([{name: 'objectId', value: id}]);
    });
});