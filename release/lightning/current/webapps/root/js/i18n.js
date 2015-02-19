
var strings = {
  parsley_messages: null,
  
  init: function( lang )
  {
    if (lang == null || lang.length == 0)
	  lang = 'en';
	
	if (strings[lang] == null) {
	  if (lang.length > 2) lang = lang.substring(0,2);
	}
	
	strings.parsley_messages = {
        defaultMessage: strings.get(lang, "This value seems to be invalid.")
      , type: {
            email:      strings.get(lang, "This value should be a valid email.")
          , url:        strings.get(lang, "This value should be a valid url.")
          , urlstrict:  strings.get(lang, "This value should be a valid url.")
          , number:     strings.get(lang, "This value should be a valid number.")
          , digits:     strings.get(lang, "This value should be digits.")
          , dateIso:    strings.get(lang, "This value should be a valid date (YYYY-MM-DD).")
          , alphanum:   strings.get(lang, "This value should be alphanumeric.")
          , phone:      strings.get(lang, "This value should be a valid phone number.")
        }
      , notnull:        strings.get(lang, "This value should not be null.")
      , notblank:       strings.get(lang, "This value should not be blank.")
      , required:       strings.get(lang, "This value is required.")
      , regexp:         strings.get(lang, "This value seems to be invalid.")
      , min:            strings.get(lang, "This value should be greater than or equal to %s.")
      , max:            strings.get(lang, "This value should be lower than or equal to %s.")
      , range:          strings.get(lang, "This value should be between %s and %s.")
      , minlength:      strings.get(lang, "This value is too short. It should have %s characters or more.")
      , maxlength:      strings.get(lang, "This value is too long. It should have %s characters or less.")
      , rangelength:    strings.get(lang, "This value length is invalid. It should be between %s and %s characters long.")
      , mincheck:       strings.get(lang, "You must select at least %s choices.")
      , maxcheck:       strings.get(lang, "You must select %s choices or less.")
      , rangecheck:     strings.get(lang, "You must select between %s and %s choices.")
      , equalto:        strings.get(lang, "This value should be the same.")
    };
	
    return lang;
  },
  geterror: function( lang, error, msg )
  {
    if (error && error.msg) {
	  var txt = error.msg.toLowerCase();
	  var errmsg = null;
	  if (txt.indexOf('not found') >= 0) {
	    if (txt.indexOf('user') >= 0) 
		  errmsg = strings.get(lang, 'User not found');
	  } else if (txt.indexOf('already existed') >= 0) {
	    if (txt.indexOf('user') >= 0) 
		  errmsg = strings.get(lang, 'User already existed');
	  }
	  
	  if (errmsg == null) 
	    errmsg = strings.get(lang, error.msg);
	  if (errmsg != null && errmsg.length > 0)
	    return errmsg;
	}
	
	return msg;
  },
  get: function( lang, text )
  {
    if (lang == null || text == null || text.length == 0)
	  return text;
	
	var ss = strings[lang];
	if (ss) {
	  var txt = ss[text];
	  if (txt != null && txt.length > 0)
	    return txt;
	}
	
	return text;
  },
  zh: {
    'Anybox - Official Site': 'Anybox - 官方网站',
	'Features': '产品特色',
	'Features Overview': '产品特色预览',
	'Web Application': '网页应用',
	'Mobile': '移动终端',
	'Pricing': '定价',
	'Support': '支持',
	'Login': '登录',
	'Sign In': '登录',
	'Sign Up': '注册',
	'Try For Free': '免费注册',
	'Service Agreement': '服务协议',
	'Privacy Policy': '隐私条款',
	'Terms of Use': '服务条款',
	'Law Enforcement': '法律条款',
	'Store. Share. Privately.': '存储. 分享. 私密.',
	'Your private cloud, store locally, access anywhere.': '您的私有云，存储本地，任意访问。',
	'Get started for free': '开始注册免费帐号吧',
	'Enter your email address...': '请输入您的电子邮件地址...',
	'Create your free Anybox account': '创建您的免费Anybox帐号',
	'Store &amp; Share': '存储 &amp; 分享',
	'Access Anywhere': '自由访问',
	'100% Private': '100% 隐私',
	'Email': '电子邮件',
	'Username': '用户名',
	'Password': '密码',
	'Re-enter Password': '再输入密码',
    'Email already taken': '电子邮件已经存在',
	'Username already taken': '用户名已经存在',
	'Welcome Back.': '欢迎回来.',
	'On the go?': '使用手机？',
	'This value should be a valid email.': '该输入项值必须为有效的邮件地址',
	'This value is required.': '该输入项不能空',
	'User not found': '用户不存在',
	'Incorrect username or password.': '错误的用户名或密码',
	'Temporary server failure. Please try again in a few minutes': '服务器暂时异常，请稍候几分钟再试。',
	'Username or password is wrong': '用户名或密码错误',
	'This field is required': '该输入项不能空',
	'Must be at least 4 characters': '该输入项值必须不少于4个字符',
	'Must be at most 38 characters': '该输入项值必须不多于38个字符',
	'Please confirm': '请确定并勾上此项',
	'Only alphanumeric characters': '只允许数字和字母',
	'Password doesn\'t match': '密码不一致',
	'Forgot Password?': '忘记密码？',
	'I understand that for complete privacy.': '我明白完整的隐私条款.',
	'Learn More': '了解更多'
  }
};
