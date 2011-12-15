$(document).ready(function() {
  // put all your jQuery goodness in here.
  label = $('#name-input-label span');
  
  $('#name-input').focus(function() {
    label.animate({marginTop: '-40px'}, 200)
    label.css('color', '#53607b')
  });

  $('#name-input').blur(function() {
    if ( $(this).val().length > 0 ) {
      label.fadeTo(100, 0)
    }
    else
    {
      label.show()
      label.animate({marginTop: '0'}, 200)
      label.fadeTo(100, 1.0)
      label.css('color', '#bbc4d7')
    }
  });
  
  $('#greet-button').click(function(e) {
    e.preventDefault();
    if ($('#name-input').val().length > 0) {
      console.log("hi")
      $('#form').animate({ opacity: 0.0,
                    marginTop: '-400px'},
                    400,
                    function() {
                      console.log('huh')
                    })
    }
  });

  if ($('#greeting').length > 0) {
    cloud = $('#greeting')
    cloud.css({ opacity: 0,
                "margin-top": '400px'})
    cloud.show()
    cloud.animate({ opacity: 1.0,
                    marginTop: '40px'}, 600)
  }

});