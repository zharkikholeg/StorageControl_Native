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

        // новая функция для парсинга ШК
        function parseBarcodeOleg(rawBarcode) {
        	const length = rawBarcode.length;
        	const firstChar = rawBarcode.charAt(0);

        	// выбираем стандартные весовые ШК на 13 символов с первой цифрой 2
        	if (length == 13 && firstChar == "2") {
        		const weightPart = parseInt(rawBarcode.slice(-6).slice(0, -1)); // получаем последние 6 символов ШК, так как в них записан вес, убираем последний, потому что там 4 знака после запятой, а нам надо только 3, приводим их к Int
        		const weightKg = weightPart / 1000; // потому что мы оставили 3 знака после запятой
        		const result = {
        			"barcodeType": "weight",
        			"weight": weightKg
        		}
        		return result;
        	}
        	// выбираем ШК с EAN-13, GTIN, могут быть как 13 (но первый символ - не 2, так как это весовой ШК), так и 14 символов
        	else if ((length == 13 && firstChar != "2") || length == 14) {
        		const result = {
        			"barcodeType": "GTIN",
        			"GTIN": rawBarcode
        		}
        		return result;
        	}
        	// DataMatrix ЧЗ, пока видел 31 и 33 знака, GTIN и индивидуальный идентификатор разделены символами "21"
        	else if ((length == 31 || length == 33) && firstChar == "0") {
    			let GTIN = rawBarcode.slice(2).slice(0, 14) // сначала убираем первые 2 символа, потом брём следующие 14
        		// убираем первый 0 из GTIN, если он есть
        		if (GTIN.charAt(0) == "0") {
        			GTIN = GTIN.slice(1)
        		}
        		const serialNo = rawBarcode.slice(18).split("\u001d")[0]; // убираем первые 18 символов, делим строку по спецсимволу и берём первую часть - это индивидуальный серийный номер
        		const hashKey = rawBarcode.slice(-4); // последние 4 имвола - крипто-ключ
        		const result = {
        			"barcodeType": "DataMatrix",
        			"GTIN": GTIN, 
        			"serialNo": serialNo,
        			"hashKey": hashKey,
        			"raw": rawBarcode
        		}
        		return result;
        	}
        	// длинные ШК с упаковок Hochland
        	else if (length == 36 && firstChar == "0") {
        		let GTIN = rawBarcode.slice(2).slice(0, 14); // это GTIN именно упаковки товара, а не 1 штуки товара; сначала убираем первые 2 символа, потом брём следующие 14
        		// убираем первый 0 из GTIN, если он есть
        		if (GTIN.charAt(0) == "0") {
        			GTIN = GTIN.slice(1)
        		}
        		const prodDateYYmmdd = rawBarcode.slice(18).slice(0, 6); // это дата производства в формате YYmmdd
        		const prodDate = moment.utc(prodDateYYmmdd, "YYMMDD").toDate(); // это дата производства в формате 2022-07-10T17:24:21.114Z
        		const batchNo = rawBarcode.slice(26);
        		const result = {
        			"barcodeType": "packHochland",
        			"GTIN": GTIN,
        			"prodDate": prodDate,
        			"batchNo": batchNo,
        			"raw": rawBarcode
        		}
        		return result;
        	}

        	// длинные ШК "Заречинские Продукты"
        	else if (length == 43 && firstChar == "0") {
        	    let GTIN = rawBarcode.slice(2).slice(0, 14); // это GTIN именно упаковки товара, а не 1 штуки товара; сначала убираем первые 2 символа, потом брём следующие 14
        	    if (GTIN.charAt(0) == "0") {
                    GTIN = GTIN.slice(1);
                }
                const weightPart = parseInt(rawBarcode.slice(20).slice(0,6)); // вес числом
                const weightKg = weightPart / 1000; // вес в кг
                const prodDateYYmmdd = rawBarcode.slice(28).slice(0,6); // это дата производства в формате YYmmdd
                const prodDate = moment.utc(prodDateYYmmdd, "YYMMDD").toDate(); // это дата производства в формате 2022-07-10T17:24:21.114Z
                const batchNo = rawBarcode.slice(36);
                const result = {
                    "barcodeType": "packZarech",
                    "GTIN": GTIN,
                    "weight": weightKg,
                    "prodDate": prodDate,
                    "batchNo": batchNo,
                    "raw": rawBarcode
        	    }
        	    return result;
        	}
        	// всё остальное
        	else {
        		const result = {
        			"barcodeType": "other",
        			"length": length,
        			"firstChar": firstChar,
        			"raw": rawBarcode
        		}
        		return result;
        	}
        }

        // запускается по событию ready на index.html
		function _run() {
		    if(_checkBarcodeScanner()) {
		        scanType = ScanType.Scanner;
		    } else {
		        scanType = ScanType.Camera;
		    }

		    // слушатель на кнопке "Сканирование"
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

			// слушатель на кнопке "Отмена"
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

		// запускается сканирование через androidInterface после нажатия кнопки "Сканирование"
		function _startScanningBarCode(scanType) {
		    if (scanType === undefined) {
		        scanType = ScanType.Camera;
		    }
		    window.androidInterface.startScanningBarCode(scanType); // обращение к интерфейсу
		    $('.scan-result').empty();
		}

		// останавливает сканирование по нажатию кновки "Отмена"
		function _stopScanningBarCode(scanType) {
		    window.androidInterface.stopScanningBarCode(scanType);
		}

		// res - сырая строка ШК из java-кода
		function _scanningBarCodeResult(res) {
		    //console.log('Scan result', res);
		    const result = parseBarcodeOleg(res)
		    $('.scan-result').html(JSON.stringify(result));
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

		// тут скорее всего передача js-колбэков в java-функции, отработавшие по завершению сканирования (resultCallback, failCallback в nativeInterface.js)
		SC.nativeInterface.scanningBarCodeResult = _scanningBarCodeResult;
		SC.nativeInterface.scanningBarCodeFailure = _scanningBarCodeFailure;

		return {
			run: _run,
			scanningBarCodeResult: _scanningBarCodeResult
		};
	})());
})();

