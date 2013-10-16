/**
 * Copyright (c) 2013, Clemens Rabe
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 3. Neither the name of SCM-Manager; nor the names of its
 *    contributors may be used to endorse or promote products derived from this
 *    software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

registerGeneralConfigPanel({
	xtype: 'configForm',
	title: 'AutoLogin',
	items: [
	        {
	        	xtype:      'textfield',
	        	fieldLabel: 'HTTP Header Variable',
	        	name:       'variable-name',
	        	helpText:   'The name of the HTTP header variable. Default value is X_REMOTE_USER.',
	        	allowBlank: false
	        },
	        {
	        	xtype:      'textfield',
	        	fieldLabel: 'Password',
	        	name:       'password',
	        	helpText:   'The password used for auto login attempts. Existing users must have this password or they will not be able to auto login.',
	        	allowBlank: false
	        },
	        {
	        	xtype:      'checkbox',
	            fieldLabel: "Allow Unknown Users",
	            name:       'allow-unknown',
	            inputValue: 'true',
	            helpText:   'If enabled, users unknown to the SCM-Manager database are allowed to log in.'
	        },
	        {
	        	xtype:      'textfield',
	        	fieldLabel: 'Email Domain',
	        	name:       'email-domain',
	        	helpText:   'The email domain for new users created by this plugin.',
	        	allowBlank: false
	        }],
	 onSubmit: function(values) {
		 this.el.mask( 'Submit ...' );
		 Ext.Ajax.request( {
			 url:            restUrl + 'config/plugins/autologin.json',
			 method:         'POST',
			 jsonData:       values,
			 scope:          this,
			 disableCaching: true,
			 success:        function( response ) {
				 this.el.unmask();
			 },
			 failure:        function() {
				 this.el.unmask();
			 }
		 } );
	 },
	 onLoad:   function(element) {
		 var tid = setTimeout( function() {
			 element.mask( 'Loading ...' );
		 }, 100 );
		 Ext.Ajax.request( {
			 url:            restUrl + 'config/plugins/autologin.json',
			 method:         'GET',
			 scope:          this,
			 disableCaching: true,
			 success:        function( response ) {
				 var obj = Ext.decode( response.responseText );
				 this.load( obj );
				 clearTimeout( tid );
				 element.unmask();
			 },
			 failure:        function() {
				 element.unmask();
				 clearTimeout( tid );
				 alert( 'failure' );
			 }
		 } );
	 }
});
