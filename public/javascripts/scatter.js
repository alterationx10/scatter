function formatDate(epoch) {
    return new Date(epoch).toLocaleString()
}

function updateHeaderStats(statUrl) {
    $.ajax({url: statUrl, success: function(result){
        $("#post-count").html(result.posts);
        $("#heart-count").html(result.hearts);
    }});
}