$(document).on('click', '.ui-diagram > .ui-diagram-element', function(info){
    // console.log('Node: ' + info.target.id);
    // console.log('Parent node: ' + info.target.parentNode.id);
    var id;
    if (info.target.id.includes('objectId')) {
        id = info.target.id.substring(info.target.id.indexOf('objectId') + 8);
    } else {
        id = info.target.parentNode.id.substring(info.target.parentNode.id.indexOf('objectId') + 8);
    }
    if (id.includes('Right')) {
        id = id.replace('Right', '');
    }
    // console.log(id);
    redirectJSF([{name: 'objectId', value: id}]);
});