window.barScanner = {};
window.SC = {
    nativeInterface: {}
};

(function() {
	$.extend(barScanner, (function() {

        var ScanType = {
            Scanner: 0,
            Camera: 1
        };

        var scanType;

		function _run() {
		    if(_checkBarcodeScanner()) {
		        scanType = ScanType.Scanner;
		    } else {
		        scanType = ScanType.Camera;
		    }

			$('.scan-btn').click(function(){
			    if(scanType === ScanType.Scanner) {
			        _startScanningBarCode(ScanType.Scanner);
			        return;
			    } else {
			        if(!_checkCameraPermissionGranted()) {
                	    console.error('Camera access denied');
                		_showError('Camera access denied');
                		return;
                    }
                    _startScanningBarCode(ScanType.Camera);
			    }
			});

			$('.close-btn').click(function(){
                _stopScanningBarCode(scanType);
            });
		}

		function _checkCameraPermissionGranted() {
		    return !!window.androidInterface.checkCameraPermissionGranted();
		}

		function _checkBarcodeScanner() {
		    return !!window.androidInterface.checkBarcodeScanner();
		}

		function _startScanningBarCode(scanType) {
		    if (scanType === undefined) {
		        scanType = ScanType.Camera;
		    }
		    window.androidInterface.startScanningBarCode(scanType);
		    $('.scan-result').empty();
		}

		function _stopScanningBarCode(scanType) {
		    window.androidInterface.stopScanningBarCode(scanType);
		}

		function _scanningBarCodeResult(res) {
		    console.log('Scan result', res);
		    $('.scan-result').html(res);
		}

		function _scanningBarCodeFailure() {
		    console.log('Scan result', 'Fail');
		    $('.scan-result').html('Fail');
		}

		function _showError(message) {
		    var $el = $('.error');
		    $el.html(message);
		    $el.removeClass('invisible');
		    setTimeout(function() {
		        $el.addClass('invisible');
		        $el.empty();
		    }, 5000);
		}

		SC.nativeInterface.scanningBarCodeResult = _scanningBarCodeResult;
		SC.nativeInterface.scanningBarCodeFailure = _scanningBarCodeFailure;

		return {
			run: _run,
			scanningBarCodeResult: _scanningBarCodeResult
		};
	})());
})();

