
$(document).ready(function() {

    var circularSlider;
    var targetTemperature;
    var currentTemperature;

    function poll(){
       setTimeout(function(){
          getTemperatures()
      }, 5000);
    };

    function getTemperatures() {
        $.ajax('/api/v1/temperatures').done(function(temperatures) {
            process(temperatures);
        }).fail(function() {
            temperature = null;
        }).always(function() {
          poll();
        });
    }

    function setTemperature(temperature) {
        $.ajax({
            method: "POST",
            url: "/api/v1/temperatures",
            contentType:"application/json; charset=utf-8",
            dataType:"json",
            data: JSON.stringify({ value: temperature })
        });
    }

    function process(temperatures) {
        var target = _.find(temperatures, function(t){ return t.name == "target"; });
        if(target) {
            if(targetTemperature != target.value) {
                targetTemperature = target.value;
                updateSlider();
            }
        } else {
            if(!targetTemperature) {
                targetTemperature = 22;
                updateSlider();
            }
            setTemperature(targetTemperature);
        }

        var sensors = _.filter(temperatures, function(t){ return t.name != "target"; });
        if(sensors.length > 0) {
            var temperature = Math.round(_.reduce(sensors, function(acc, t){ return acc + t.value; }, 0) / sensors.length);
            if(temperature != currentTemperature) {
                currentTemperature = temperature;
                updateSlider();
            }

        }
    }

    function updateSlider() {
        if(!circularSlider) {
            circularSlider = $('#content').CircularSlider({
                min : 10,
                max: 70,
                radius: 300,
                innerCircleRatio: 0.8,
                value : targetTemperature,
                formLabel : function(value, prefix, suffix) {
                  var label = "";
                  if(currentTemperature) {
                    label = currentTemperature + "°/"
                  }
                  label += value + "°"
                  return label;
                },
                slide : function(ui, value) {
                  if(targetTemperature != value) {
                    targetTemperature = value;
                    setTemperature(value);
                  }
                }
            });
        }

        circularSlider.setValue(targetTemperature);
    }

    getTemperatures();
});
