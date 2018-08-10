exports.defineAutoTests = function() {
    "use strict";

    function removeTextFile(name, success, onError) {
        window.resolveLocalFileSystemURL(cordova.file.externalRootDirectory, onDirReady, onError);

        function onDirReady(dir) {
            dir.getFile(name, {
                create: false
            }, onFileEntry, onError);
        }

        function onFileEntry(fileEntry) {
            fileEntry.remove(success, onError);
        }
    }

    function writeTextFile(name, size, success, onError) {
        window.resolveLocalFileSystemURL(cordova.file.externalRootDirectory, onDirReady, onError);

        var rawData = "";
        var alphabet = "0123456789abcdefghijklmnopqrstuvwxyz";
        var fileUrl = null;

        for (var i = 0; i < size; ++i) {
            rawData += alphabet.charAt((Math.random() * alphabet.length) | 0);
        }

        function onDirReady(dir) {
            dir.getFile(name, {
                create: true
            }, onFileEntry, onError);
        }

        function onFileEntry(fileEntry) {
            fileUrl = fileEntry.toURL();
            fileEntry.createWriter(onFileWriter, onError);
        }

        function onFileWriter(fileWriter) {
            fileWriter.onwrite = function(event) {
                success(rawData.length, fileUrl, rawData);
            };
            fileWriter.onerror = function(event) {
                onError(0);
            };
            fileWriter.write(rawData);
        }
    }

    function generateUUID() {
        var d = new Date().getTime();
        if (window.performance && typeof window.performance.now === "function") {
            d += performance.now(); //use high-precision timer if available
        }
        var uuid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
            var r = (d + Math.random() * 16) % 16 | 0;
            d = Math.floor(d / 16);
            return (c == 'x' ? r : (r & 0x3 | 0x8)).toString(16);
        });
        return uuid;
    }

    function getObjectWithOwnProps(obj){
        var r = {};
        for(var name in obj){
            if(obj.hasOwnProperty(name)){
                r[name] = obj[name];
            }
        }
        return r;
    }

    describe('Test components', function() {
        it('should write and delete file of particular size', function(done) {
            writeTextFile("test.txt", 1024, function(written, url, rawData) {
                expect(written).toBe(1024);
                expect(url).toContain("test.txt");
                expect(rawData).not.toBeUndefined();
                removeTextFile("test.txt", function() {
                    expect("ok").toBe("ok");
                    done();
                }, function() {
                    expect("false").toBe("ok");
                });
            }, function(written) {
                expect(written).toBe(1024);
                done();
            });
        });


    });

    describe('Completion statuses', function(){
        it('should have completion status ALL', function(){
            expect(friendlysol.FsCompletionStatus.ALL).toBe('all');
        });

        it('should have completion status NOT_COMPLETED', function(){
            expect(friendlysol.FsCompletionStatus.NOT_COMPLETED).toBe('not_completed');
        });

        it('should have completion status COMPLETED', function(){
            expect(friendlysol.FsCompletionStatus.COMPLETED).toBe('completed');
        });

        it('should have completion status COMPLETED_WITH_ERROR', function(){
            expect(friendlysol.FsCompletionStatus.COMPLETED_WITH_ERROR).toBe('completed_with_error');
        });

        it('should have completion status COMPLETED_WITH_ERROR_ATTEMPTS_EXCEED', function(){
            expect(friendlysol.FsCompletionStatus.COMPLETED_WITH_ERROR_ATTEMPTS_EXCEED).toBe('completed_with_error_attempts_exceed');
        });

        it('should have completion status COMPLETED_WITH_SUCCESS', function(){
            expect(friendlysol.FsCompletionStatus.COMPLETED_WITH_SUCCESS).toBe('completed_with_success');
        });
    });
    
    describe('Error types', function(){
        it('should have error type MALFORMED_URL', function(){
            expect(friendlysol.FsUploadErrorType.MALFORMED_URL).toBe('malformed_url');
        });

        it('should have error type REQUEST_WRITE_FAILURE', function(){
            expect(friendlysol.FsUploadErrorType.REQUEST_WRITE_FAILURE).toBe('request_write_failure');
        });

        it('should have error type MAX_ATTEMPTS_REACHED', function(){
            expect(friendlysol.FsUploadErrorType.MAX_ATTEMPTS_REACHED).toBe('max_attempts_reached');
        });

        it('should have error type FILE_NOT_FOUND', function(){
            expect(friendlysol.FsUploadErrorType.FILE_NOT_FOUND).toBe('file_not_found');
        });

        it('should have error type FILE_READ_FAILURE', function(){
            expect(friendlysol.FsUploadErrorType.FILE_READ_FAILURE).toBe('file_read_failure');
        });

        it('should have error type INVALID_REQUEST', function(){
            expect(friendlysol.FsUploadErrorType.INVALID_REQUEST).toBe('invalid_request');
        });

        it('should have error type INVALID_REQUEST_PARAMS', function(){
            expect(friendlysol.FsUploadErrorType.INVALID_REQUEST_PARAMS).toBe('invalid_request_params');
        });

        it('should have error type UPLOAD_IO_FAILURE', function(){
            expect(friendlysol.FsUploadErrorType.UPLOAD_IO_FAILURE).toBe('upload_io_failure');
        });

        it('should have error type RESPONSE_READ_FAILURE', function(){
            expect(friendlysol.FsUploadErrorType.RESPONSE_READ_FAILURE).toBe('response_read_failure');
        });

        it('should have error type RESPONSE_ERROR', function(){
            expect(friendlysol.FsUploadErrorType.RESPONSE_ERROR).toBe('response_error');
        });

        it('should have error type INTERNAL_PLUGIN_ERROR', function(){
            expect(friendlysol.FsUploadErrorType.INTERNAL_PLUGIN_ERROR).toBe('internal_plugin_error');
        });
    });    

    describe('Callback management', function() {
        it('should setUploadCallback', function(done) {
            friendlysol.FsUpload.setUploadCallback(function(data) {
                expect(data).toBe("ok");
                done();
            }, function() {
                expect("").toBe("ok");
                done();
            });
        });

        it('should removeUploadCallback', function(done) {
            friendlysol.FsUpload.removeUploadCallback(function() {
                expect("ok").toBe("ok");
                done();
            }, function() {
                expect("false").toBe("ok");
                done();
            });
        });
    });


    describe('Transport timeouts management', function() {
        it('should get positive connect timeout', function(done) {
            friendlysol.FsUpload.getConnectTimeout(function(timeout) {
                expect(timeout).toBeGreaterThan(0);
                done();
            }, function() {
                expect("false").toBe("ok");
                done();
            });
        });

        it('should get positive socket timeout', function(done) {
            friendlysol.FsUpload.getSocketTimeout(function(timeout) {
                expect(timeout).toBeGreaterThan(0);
                done();
            }, function() {
                expect("false").toBe("ok");
                done();
            });
        });

        it('should setConnectTimeout and read same', function(done) {
            function onError(exc) {
                expect(exc).toBe("true");
                done();
            };

            friendlysol.FsUpload.setConnectTimeout(15000, function() {
                friendlysol.FsUpload.getConnectTimeout(function(timeout) {
                    expect(timeout).toBe(15000);
                    done();
                }, onError);
            }, onError);
        });

        it('should setSocketTimeout and read same', function(done) {
            function onError(exc) {
                expect(exc).toBe("true");
                done();
            };

            friendlysol.FsUpload.setSocketTimeout(47000, function() {
                friendlysol.FsUpload.getSocketTimeout(function(timeout) {
                    expect(timeout).toBe(47000);
                    done();
                }, onError);
            }, onError);
        });

        it('should handle setSocketTimeout invalid value', function(done) {
            function onSuccess() {
                expect("false").toBe("true");
                done();
            };
            friendlysol.FsUpload.setSocketTimeout(-1, onSuccess, function(error) {
                expect(error).toBe("timeout must be greater than zero");
                done();
            });
        });

        it('should handle setConnectTimeout invalid value', function(done) {
            function onSuccess() {
                expect("false").toBe("true");
                done();
            };
            friendlysol.FsUpload.setConnectTimeout(-1, onSuccess, function(error) {
                expect(error).toBe("timeout must be greater than zero");
                done();
            });
        });
    });

    describe('FsUploadRequest.start()', function() {

        var request;
        var fileUrl;
        var emptyFileUrl;
        var fileData;

        beforeAll(function(done) {
            writeTextFile("upload_text.txt", 512/* * 1024*/, function(size, url, rawData) {
                expect(size).toBe(512/* * 1024*/);
                expect(url).toContain("upload_text.txt");
                expect(rawData).not.toBeUndefined();
                fileUrl = url;
                fileData = rawData;
                writeTextFile("upload_empty.txt", 0, function(size, url, rawData) {
                    expect(size).toBe(0);
                    expect(url).toContain("upload_empty.txt");
                    expect(rawData).toBe("");
                    emptyFileUrl = url;
                    done();
                }, function() {
                    expect("false").toBe("ok");
                    done();
                });
            }, function() {
                expect("false").toBe("ok");
                done();
            });
        });

        afterAll(function(done) {
            function onError(err){
                expect(err).toBe("ok");
                done();
            }

            function ensureEmptyDB(ids){
                expect(ids).not.toBeUndefined();
                expect(ids.length).toBeGreaterThan(0);
                friendlysol.FsUpload.getUploads(null, friendlysol.FsCompletionStatus.ALL, function(uploads){
                    expect(uploads).not.toBeUndefined();
                    expect(uploads.length).toBe(0);
                    done();
                }, onError);
            }

            function removeUploads(){
                friendlysol.FsUpload.deleteUploads(null, friendlysol.FsCompletionStatus.ALL, ensureEmptyDB, onError);
            }

            removeTextFile("upload_text.txt", function() {
                removeTextFile("upload_empty.txt", removeUploads, onError)
            }, onError);
        });

        beforeEach(function() {
            expect(fileUrl).not.toBeUndefined();
            request = new friendlysol.FsUploadRequest(generateUUID(), "http://httpbin.org/post", fileUrl, "upload_test.txt");
        });

        it('should handle empty id', function(done) {
            request.id = null;
            request.start(function() {
                expect("false").toBe("ok");
                done();
            }, function(error) {
                expect(error).toBe("empty request id is not allowed");
                done();
            });
        });

        it('should handle whitespace id', function(done) {
            request.id = "          \t";
            request.start(function() {
                expect("false").toBe("ok");
                done();
            }, function(error) {
                expect(error).toBe("empty request id is not allowed");
                done();
            });
        });

        it('should handle empty url', function(done) {
            request.url = null;
            request.start(function() {
                expect("false").toBe("ok");
                done();
            }, function(error) {
                expect(error).toBe("invalid url");
                done();
            });
        });

        it('should handle whitespace url', function(done) {
            request.url = "     \t";
            request.start(function() {
                expect("false").toBe("ok");
                done();
            }, function(error) {
                expect(error).toBe("invalid url");
                done();
            });
        });

        it('should handle invalid url scheme', function(done) {
            request.url = "httpx://dhshf.dskfh";
            request.start(function() {
                expect("false").toBe("ok");
                done();
            }, function(error) {
                expect(error).toBe("invalid url");
                done();
            });
        });

        it('should handle malformed url', function(done) {
            request.url = "http://dhshf.....dskfh";
            request.start(function() {
                expect("false").toBe("ok");
                done();
            }, function(error) {
                expect(error).toBe("invalid url");
                done();
            });
        });

        it('should handle null file path', function(done) {
            request.filepath = null;
            request.start(function() {
                expect("false").toBe("ok");
                done();
            }, function(error) {
                expect(error).toBe("filepath can not be empty or null");
                done();
            });
        });

        it('should handle whitespace file path', function(done) {
            request.filepath = "      \t";
            request.start(function() {
                expect("false").toBe("ok");
                done();
            }, function(error) {
                expect(error).toBe("filepath can not be empty or null");
                done();
            });
        });

        it('should handle directory path', function(done) {
            request.filepath = request.filepath.replace("/upload_text.txt", "");
            request.start(function() {
                expect("false").toBe("ok");
                done();
            }, function(error) {
                expect(error).toBe("non file path");
                done();
            });
        });

        it('should handle non existing file', function(done) {
            request.filepath = request.filepath.replace("upload_text.txt", "upload_text.txt2");
            request.start(function() {
                expect("false").toBe("ok");
                done();
            }, function(error) {
                expect(error).toBe("file does not exist");
                done();
            });
        });

        it('should handle empty file', function(done) {
            request.filepath = emptyFileUrl;
            request.start(function() {
                expect("false").toBe("ok");
                done();
            }, function(error) {
                expect(error).toBe("can not upload zero length file");
                done();
            });

        });

        it('should handle empty filename', function(done) {
            request.filename = null;
            request.start(function() {
                expect("false").toBe("ok");
                done();
            }, function(error) {
                expect(error).toBe("filename can not be empty");
                done();
            });
        });

        it('should handle whitespace filename', function(done) {
            request.filename = "     \t";
            request.start(function() {
                expect("false").toBe("ok");
                done();
            }, function(error) {
                expect(error).toBe("filename can not be empty");
                done();
            });
        });

        it('should handle empty mimetype', function(done) {
            request.mimetype = null;
            request.start(function() {
                expect("false").toBe("ok");
                done();
            }, function(error) {
                expect(error).toBe("mimetype can not be empty");
                done();
            });
        });

        it('should handle whitespace mimetype', function(done) {
            request.mimetype = '     \t';
            request.start(function() {
                expect("false").toBe("ok");
                done();
            }, function(error) {
                expect(error).toBe("mimetype can not be empty");
                done();
            });
        });

        it('should handle malformed mimetype', function(done) {
            request.mimetype = 'application//x/d/c';
            request.start(function() {
                expect("false").toBe("ok");
                done();
            }, function(error) {
                expect(error).toBe("invalid mimetype");
                done();
            });
        });

        it('should handle malformed mimetype 2', function(done) {
            request.mimetype = 'super-duper';
            request.start(function() {
                expect("false").toBe("ok");
                done();
            }, function(error) {
                expect(error).toBe("invalid mimetype");
                done();
            });
        });

        it('should handle invalid addParam parameters', function() {
            expect(function(){
                request.addParam(null, null);
            }).toThrowError(TypeError, "name can not be null");

            expect(function(){
                request.addParam("param0", null);
            }).toThrowError(TypeError, "value can not be null");

            expect(function(){
                request.addParam("", "test");
            }).toThrowError(TypeError, "invalid param name: ");

            expect(function(){
                request.addParam("param0", "");
            }).toThrowError(TypeError, "param value can not be empty");

            expect(function(){
                request.addParam("{test}", "test");
            }).toThrowError(TypeError, "invalid param name: {test}");

            expect(function(){
                request.addParam("łączka", "test");
            }).toThrowError(TypeError, "invalid param name: łączka");

        });

        it('should handle invalid addHeader parameters', function() {
            expect(function(){
                request.addHeader(null, null);
            }).toThrowError(TypeError, "name can not be null");

            expect(function(){
                request.addHeader("param0", null);
            }).toThrowError(TypeError, "value can not be null");

            expect(function(){
                request.addHeader("", "test");
            }).toThrowError(TypeError, "invalid header name: ");

            expect(function(){
                request.addHeader("param0", "");
            }).toThrowError(TypeError, "header value can not be empty");

            expect(function(){
                request.addHeader("{test}", "test");
            }).toThrowError(TypeError, "invalid header name: {test}");

            expect(function(){
                request.addHeader("łączka", "test");
            }).toThrowError(TypeError, "invalid header name: łączka");

            expect(function(){
                request.addHeader("param0", "aksa\taks");
            }).not.toThrowError(TypeError, "invalid header value: aksa\taks");

            expect(function(){
                request.addHeader("param0", "\taksa\taks");
            }).toThrowError(TypeError, "invalid header value: \taksa\taks");

            expect(function(){
                request.addHeader("param0", "łączka");
            }).toThrowError(TypeError, "invalid header value: łączka");

        });

        it('should upload file without extra data', function(done) {
            request.start(function(rq) {
                //needed as it is not included in callback request
                delete request.data;
                delete request.headers;
                expect(rq).toEqual(jasmine.objectContaining(getObjectWithOwnProps(request)));
                expect(rq.error).toBeUndefined();
                expect(rq.response).not.toBeUndefined();
                expect(rq.response.status).toBe(200);
                expect(rq.response.data).not.toBeUndefined();
                expect(rq.response.data.files).not.toBeUndefined();
                expect(rq.response.data.files.file).toBe(fileData);
                done();
            }, function(error) {
                expect(error).toBeUndefined();
                done();
            });
        }, 15000);

        it('should upload file with extra data', function(done) {
            request.addParam('param0', 'abra');
            request.addParam('param1', 'cadabra');
            request.addParam('param2', 'zażółć gęślą jaźń');
            request.start(function(rq) {
                //needed as it is not included in callback request
                delete request.data;
                delete request.headers;
                expect(rq).toEqual(jasmine.objectContaining(getObjectWithOwnProps(request)));
                expect(rq.error).toBeUndefined();
                expect(rq.response).not.toBeUndefined();
                expect(rq.response.status).toBe(200);
                expect(rq.response.data).not.toBeUndefined();
                expect(rq.response.data.files).not.toBeUndefined();
                expect(rq.response.data.files.file).toBe(fileData);
                expect(rq.response.data.form).not.toBeUndefined();
                expect(rq.response.data.form.param0).toBe('abra');
                expect(rq.response.data.form.param1).toBe('cadabra');
                expect(rq.response.data.form.param2).toBe('zażółć gęślą jaźń');
                done();
            }, function(error) {
                expect(error).toBeUndefined();
                done();
            });
        }, 15000);

        it('should upload file with extra headers', function(done) {
            request.addHeader('X-Custom-Data0', 'abra');
            request.addHeader('X-Custom-Data1', 'cadabra');
            request.addHeader('X-Custom-Data2', 'texting');
            request.start(function(rq) {
                //needed as it is not included in callback request
                delete request.data;
                delete request.headers;
                expect(rq).toEqual(jasmine.objectContaining(getObjectWithOwnProps(request)));
                expect(rq.error).toBeUndefined();
                expect(rq.response).not.toBeUndefined();
                expect(rq.response.status).toBe(200);
                expect(rq.response.data).not.toBeUndefined();
                expect(rq.response.data.files).not.toBeUndefined();
                expect(rq.response.data.files.file).toBe(fileData);
                expect(rq.response.data.headers).not.toBeUndefined();
                expect(rq.response.data.headers['X-Custom-Data0']).toBe('abra');
                expect(rq.response.data.headers['X-Custom-Data1']).toBe('cadabra');
                expect(rq.response.data.headers['X-Custom-Data2']).toBe('texting');
                done();
            }, function(error) {
                expect(error).toBeUndefined();
                done();
            });
        }, 15000);

        it('should upload file with extra headers and params', function(done) {
            request.addHeader('X-Custom-Data0', 'abra');
            request.addHeader('X-Custom-Data1', 'cadabra');
            request.addHeader('X-Custom-Data2', 'texting');
            request.addParam('param0', 'abra');
            request.addParam('param1', 'cadabra');
            request.addParam('param2', 'zażółć gęślą jaźń');
            request.start(function(rq) {
                //needed as it is not included in callback request
                delete request.data;
                delete request.headers;
                expect(rq).toEqual(jasmine.objectContaining(getObjectWithOwnProps(request)));
                expect(rq.error).toBeUndefined();
                expect(rq.response).not.toBeUndefined();
                expect(rq.response.status).toBe(200);
                expect(rq.response.data).not.toBeUndefined();
                expect(rq.response.data.files).not.toBeUndefined();
                expect(rq.response.data.files.file).toBe(fileData);
                expect(rq.response.data.headers).not.toBeUndefined();
                expect(rq.response.data.headers['X-Custom-Data0']).toBe('abra');
                expect(rq.response.data.headers['X-Custom-Data1']).toBe('cadabra');
                expect(rq.response.data.headers['X-Custom-Data2']).toBe('texting');
                expect(rq.response.data.form).not.toBeUndefined();
                expect(rq.response.data.form.param0).toBe('abra');
                expect(rq.response.data.form.param1).toBe('cadabra');
                expect(rq.response.data.form.param2).toBe('zażółć gęślą jaźń');
                done();
            }, function(error) {
                expect(error).toBeUndefined();
                done();
            });
        }, 15000);
    });

    describe('Upload queue management', function(){
        var request;
        var fileUrl;
        var emptyFileUrl;
        var fileData;
        var requests;

        function createNewRequest(){
            var rq = new friendlysol.FsUploadRequest(generateUUID(), "http://httpbin.org/post", fileUrl, "upload_test.txt");
            requests.push(rq.id);
            return rq;
        }

        beforeAll(function(done) {
            writeTextFile("upload_text.txt", 512/* * 1024*/, function(size, url, rawData) {
                expect(size).toBe(512/* * 1024*/);
                expect(url).toContain("upload_text.txt");
                expect(rawData).not.toBeUndefined();
                fileUrl = url;
                fileData = rawData;
                writeTextFile("upload_empty.txt", 0, function(size, url, rawData) {
                    expect(size).toBe(0);
                    expect(url).toContain("upload_empty.txt");
                    expect(rawData).toBe("");
                    emptyFileUrl = url;
                    done();
                }, function() {
                    expect("false").toBe("ok");
                    done();
                });
            }, function() {
                expect("false").toBe("ok");
                done();
            });
        });

        afterAll(function(done) {
            function onError(err){
                expect(err).toBe("ok");
                done();
            }

            function ensureEmptyDB(ids){
                expect(ids).not.toBeUndefined();
                friendlysol.FsUpload.getUploads(null, friendlysol.FsCompletionStatus.ALL, function(uploads){
                    expect(uploads).not.toBeUndefined();
                    expect(uploads.length).toBe(0);
                    done();
                }, onError);
            }

            function removeUploads(){
                friendlysol.FsUpload.deleteUploads(null, friendlysol.FsCompletionStatus.ALL, ensureEmptyDB, onError);
            }

            removeTextFile("upload_text.txt", function() {
                removeTextFile("upload_empty.txt", removeUploads, onError)
            }, onError);
        });


        beforeEach(function() {
            requests = [];
            expect(fileUrl).not.toBeUndefined();
            request = new friendlysol.FsUploadRequest(generateUUID(), "http://httpbin.org/post", fileUrl, "upload_test.txt");
        });

        afterEach(function(done){
            var rcx = requests.concat([request.id]);
            friendlysol.FsUpload.deleteUploads(rcx, friendlysol.FsCompletionStatus.ALL, function(ids){
                expect(ids).not.toBeUndefined();
                expect(ids.length).toBe(rcx.length);
                friendlysol.FsUpload.removeUploadCallback(done, function(error){
                    expect(error).toBe("ok");
                    done();
                });
            }, function(error){
                expect(error).toBe("ok");
                done();
            });
        });

        it('should upload file and have completed upload in db', function(done){
            function onError(error){
                expect(error).toBe("ok");
                done();
            }

            function onUploadDone(rq){
                friendlysol.FsUpload.getUploads(null,
                                                friendlysol.FsCompletionStatus.COMPLETED_WITH_SUCCESS,
                                                function(arr){
                                                    expect(arr).not.toBeUndefined();
                                                    expect(arr.length).toBe(1);
                                                    expect(arr[0]).toEqual(jasmine.objectContaining(rq));
                                                    expect(arr[0].completed).toBe(true);
                                                    expect(arr[0].success).toBe(true);
                                                    done();
                                                }, onError);
            }

            request.start(onUploadDone, onError);
        }, 15000);

        it('should upload multiple files and have completed uploads in db', function(done){
            var started = {};
            var keys = 0;

            function checkComplete(rq){
                started[rq.id] = rq;
                keys++;
                for(var id in started){
                    if(started[id] == null){
                        return false;
                    }
                }
                return true;
            }

            function onError(error){
                if(checkComplete(error)){
                    done();
                }
            }

            function onUploadDone(rq){
                if(checkComplete(rq)){
                    friendlysol.FsUpload.getUploads(null,
                                                    friendlysol.FsCompletionStatus.COMPLETED_WITH_SUCCESS,
                                                    function(arr){
                                                        expect(arr).not.toBeUndefined();
                                                        expect(arr.length).toBe(keys);
                                                        var uploadData;
                                                        var rqData;
                                                        for(var i = 0; i < arr.length; ++i){
                                                            uploadData = arr[i];
                                                            expect(uploadData).not.toBeUndefined();
                                                            expect(uploadData.id).not.toBeUndefined();
                                                            rqData = started[uploadData.id];
                                                            expect(rqData).not.toBeUndefined();
                                                            expect(uploadData).toEqual(jasmine.objectContaining(rqData));
                                                            expect(uploadData.response).not.toBeUndefined();
                                                            expect(uploadData.response.status).toBe(200);
                                                            expect(uploadData.response.data).not.toBeUndefined();
                                                            expect(uploadData.response.data.files).not.toBeUndefined();
                                                            expect(uploadData.response.data.files.file).toBe(fileData);
                                                            expect(uploadData.completed).toBe(true);
                                                            expect(uploadData.success).toBe(true);
                                                        }
                                                        done();
                                                    }, function(err){
                                                        expect(err).toBe("ok");
                                                        done();
                                                    });
                }
            }
            started[request.id] = null;
            request.start(onUploadDone, onError);
            for(var i = 0; i < 5; ++i){
                var rq = createNewRequest();
                started[rq.id] = null;
                rq.start(onUploadDone, onError);
            }
        }, 25000);


        it('should upload multiple files and reupload by reset with global callback usage', function(done){
            var started = {};
            var restarted = {};
            var keys = 0;
            var restartedKeys = 0;
            var progressCalls = 0;

            function checkComplete(rq){
                started[rq.id] = rq;
                keys++;
                for(var id in started){
                    if(started[id] == null){
                        return false;
                    }
                }
                return true;
            }

            function checkCompleteRestart(rq){
                if(rq.progress != null){
                    expect(rq.progress).not.toBeLessThan(0);
                    expect(rq.progress).not.toBeGreaterThan(1);
                    progressCalls++;
                    return false;
                }
                restarted[rq.id] = rq;
                restartedKeys++;
                for(var id in restarted){
                    if(restarted[id] == null){
                        return false;
                    }
                }
                return true;
            }

            function onErrorRestart(error){
                if(checkCompleteRestart(error)){
                    done();
                }
            }

            function onError(error){
                if(checkComplete(error)){
                    done();
                }
            }

            function globalCallback(indata){
                if(checkCompleteRestart(indata)){
                    friendlysol.FsUpload.getUploads(null,
                                                    friendlysol.FsCompletionStatus.COMPLETED_WITH_SUCCESS,
                                                    function(arr){
                                                        expect(arr).not.toBeUndefined();
                                                        expect(arr.length).toBe(restartedKeys);
                                                        var uploadData;
                                                        var rqData;
                                                        for(var i = 0; i < arr.length; ++i){
                                                            uploadData = arr[i];
                                                            expect(uploadData).not.toBeUndefined();
                                                            expect(uploadData.id).not.toBeUndefined();
                                                            rqData = restarted[uploadData.id];
                                                            expect(rqData).not.toBeUndefined();
                                                            expect(uploadData).toEqual(jasmine.objectContaining(rqData));
                                                            expect(uploadData.response).not.toBeUndefined();
                                                            expect(uploadData.response.status).toBe(200);
                                                            expect(uploadData.response.data).not.toBeUndefined();
                                                            expect(uploadData.response.data.files).not.toBeUndefined();
                                                            expect(uploadData.response.data.files.file).toBe(fileData);
                                                            expect(uploadData.completed).toBe(true);
                                                            expect(uploadData.success).toBe(true);
                                                        }
                                                        expect(progressCalls).toBeGreaterThan(0);
                                                        done();
                                                    }, function(err){
                                                        expect(err).toBe("ok");
                                                        done();
                                                    });
                }
            }

            function makeResetPhase(){
                friendlysol.FsUpload.resetUploads(null,
                                                  friendlysol.FsCompletionStatus.COMPLETED_WITH_SUCCESS,
                                                  function(ids){
                                                     expect(ids).not.toBeUndefined();
                                                     expect(ids.length).toBe(keys);
                                                  }, function(err){
                                                      expect(err).toBe("ok");
                                                      done();
                                                  });
            }

            function onUploadsDone(){
                friendlysol.FsUpload.getUploads(null,
                                                friendlysol.FsCompletionStatus.COMPLETED_WITH_SUCCESS,
                                                function(arr){
                                                    expect(arr).not.toBeUndefined();
                                                    expect(arr.length).toBe(keys);
                                                    var uploadData;
                                                    var rqData;
                                                    for(var i = 0; i < arr.length; ++i){
                                                        uploadData = arr[i];
                                                        expect(uploadData).not.toBeUndefined();
                                                        expect(uploadData.id).not.toBeUndefined();
                                                        rqData = started[uploadData.id];
                                                        expect(rqData).not.toBeUndefined();
                                                        expect(uploadData).toEqual(jasmine.objectContaining(rqData));
                                                        expect(uploadData.response).not.toBeUndefined();
                                                        expect(uploadData.response.status).toBe(200);
                                                        expect(uploadData.response.data).not.toBeUndefined();
                                                        expect(uploadData.response.data.files).not.toBeUndefined();
                                                        expect(uploadData.response.data.files.file).toBe(fileData);
                                                        expect(uploadData.completed).toBe(true);
                                                        expect(uploadData.success).toBe(true);
                                                    }
                                                    friendlysol.FsUpload.setUploadCallback(function(d){
                                                        if(d === "ok"){
                                                            makeResetPhase();
                                                        }else{
                                                            globalCallback(d);
                                                        }
                                                    }, onErrorRestart);
                                                }, function(err){
                                                    expect(err).toBe("ok");
                                                    done();
                                                });
            }

            function onUploadDone(rq){
                if(checkComplete(rq)){
                    onUploadsDone();
                }
            }
            started[request.id] = null;
            request.start(onUploadDone, onError);
            for(var i = 0; i < 5; ++i){
                var rq = createNewRequest();
                started[rq.id] = null;
                rq.start(onUploadDone, onError);
            }
        }, 25000);
    });
};