package main.java.com.wp.carlos4web.vraptor.js.i18n;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;

import org.apache.log4j.Logger;

import br.com.caelum.vraptor.http.InvalidParameterException;
import br.com.caelum.vraptor.ioc.ApplicationScoped;
import br.com.caelum.vraptor.ioc.Component;

/**
 * This component exports all supported locales of application. When the application starts
 * the .properties files will be exported to javascript files with content in jQuery plugin
 * format.
 * <br/>
 * To configure this component, will be necessary add the 'locales' parameter in the web.xml, 
 * passing all application locales splited by commas. E.g: pt,pt_BR,en,es
 * <br/>
 * All javascript files will be deployed to WebContent/js/i18n/, the origin of .properties
 * follow de Java specification for resources files, resources/*.properties.
 * <br/>
 * The sintax of jQuery plugin its the same of fmt:message tag, but for jQuery:
 * <br/>
 * <code>alert ( $.msg( "you.key.of.propertie.file" ) );</code>
 * <br/>
 * When the plugins not found your key, the output will be between '?? XYZ ??', the same of
 * fmt:message tag.
 * <br/>
 * To import the Javascript file use this:
 * <br/>
 * <code>
 * <script type="text/javascript" src="<c:url value='/js/i18n/messages_${YOUR_LOCALE}.js'/>"></script>
 * </code>
 * 
 * @author Carlos A. Junior (CIH - Centro Internacional de Hidroinformática - carlosjrcabello@gmail.com)
 * 
 * @see ProtectionDomain
 * 
 * @see CodeSource
 */
@Component
@ApplicationScoped
public class ResourceBundleJavascriptExport
{
	private static final Logger logger = Logger.getLogger(ResourceBundleJavascriptExport.class);
	
	/**
	 * Instance of application parameters.
	 */
	private ServletContext context;
	
	/**
	 * Default construtor with dependency injection of ServletContext object.
	 * 
	 * @param context
	 */
	public ResourceBundleJavascriptExport (ServletContext context)
	{
		this.context = context;
	}
	
	/**
	 * Returns an List of Locale objects based in the 'locales' parameter of web.xml. All
	 * locales specified should be separated by commas.<br/> <br/> 
	 * 
	 * <code>E.g: pt_BR,en,es</code>
	 * 
	 * @return List
	 * 
	 * @throws InvalidParameterException - when 'locales' parameter is null or empty.
	 */
	public List<Locale> getApplicationLocales()
	{
		String s = this.context.getInitParameter("locales");
		
		String[] languages = s != null ? s.split(",") : null;
		
		if(languages == null || languages.length == 0)
		{
			throw new InvalidParameterException("Could not find any languages in web.xml 'locales' parameter.");
		}
		
		List<Locale> locales = new ArrayList<Locale>();
		
		for (String lang : languages)
		{
			locales.add(new Locale(lang));
		}
		
		return locales;
	}
	
	/**
	 * Deploy an new Javascript file based in the content of ResourceBundle
	 * object. The target folder is WEB-INF/js/i18n/. All files using the
	 * current charset of 'br.com.caelum.vraptor.encoding' parameter.
	 * 
	 * @param File properties
	 * 
	 * @throws IOException - throws when the target dir could not be created 
	 * or Javascript file creation fail.
	 * 
	 * @see web.xml 'br.com.caelum.vraptor.encoding' parameter
	 */
	private void deployI18nJavascriptFile(ResourceBundle bundle)
	{
		if(bundle != null)
		{
			try
			{
				Enumeration<String> enumeration = bundle.getKeys();
				
				String keys = "\t\t";
				int i = 0;
				while(enumeration.hasMoreElements())
				{
					String key = enumeration.nextElement();
					
					if(i > 0 && i%3 == 0)
					{
						keys += "\n\t\t";
					}
					
					// Fix quotation marks and break lines of an String.
					String traducao = bundle.getString(key).replaceAll("'", "\'");
					traducao = traducao.replaceAll("\\n", "\\\\n");
					
					keys += "m[\"" + key + "\"] = \"" + traducao + "\";\n\t\t";
					i++;
				}
				
				if(keys.length() > 2)
				{
					if(keys.endsWith("\t\t"))
					{
						keys = keys.replaceAll("\t\t$", "\t");
					}
					
					String plugin 	 = "jQuery( function($)\n";
					plugin 			+= "{\n";
					plugin 			+= "\t$.msg = function (key)\n";
					plugin 			+= "\t{\n";
					plugin 			+= "\t\tvar m = new Array();\n\n";
					plugin 			+= keys;
					plugin			+= "\n\t\tvar msg = m[key];\n\t";
					plugin			+= "\n\t\tmsg = (msg == undefined) ? ('??[' + key + ']??') : msg;\n\t";
					plugin			+= "\n\t\treturn msg;\n\t";
					plugin 			+= "}\n";
					plugin 			+= "});";
					
					// Write content to new Javascript file.
					this.createI18nJavascriptFile(plugin, bundle.getLocale());
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * This method creates an new Javascript file with the jQuery plugin content ($.msg). If
	 * the file exists, the same will be replaced.
	 * 
	 * @param content - The jQuery plugin content.
	 * 
	 * @param locale - Locale to set a name of file. E.g: messages_pt_BR.properties
	 * 
	 * @throws IOException - For file operations.
	 */
	private void createI18nJavascriptFile (String content, Locale locale) throws IOException
	{
		// Ensures that the file is at the root of WebContent
		File root = new File(this.context.getRealPath("/"));
		
		File dir = new File(
			root.getCanonicalPath() + File.separator + "js" + File.separator + "i18n" + File.separator
		);
		
		if(!dir.exists())
		{
			dir.mkdirs();
		}
		
		if(!dir.exists())
		{
			throw new IOException("Could not create the destination folder of i18n javascript files.");
		}
		
		File js = new File(
			root.getCanonicalPath() + File.separator + "js" + File.separator + "i18n" + File.separator + "messages_" + locale + ".js"
		);
		
		// Delete and create.
		if(js.exists())
		{
			js.delete();
		}
		js.createNewFile();
		
		String charset = this.context.getInitParameter("br.com.caelum.vraptor.encoding");
		
		if(charset == null || charset.isEmpty())
		{
			charset = "UTF-8";
		}
		
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(js), charset));
        
        bw.write(content);
        bw.close();
        logger.info("The file " + js.getName() + " was deployed successfully.");
	}
	
	/**
	 * This method is automatically invoked by conteiner after the instantiation of this 
	 * class across @PostConstruct annotation. 
	 */
	@PostConstruct
	public void initI18nJavascriptFilesDeploy ()
	{
		logger.info("Generating the Javascript files for i18n.");
		try
		{
			List<Locale> locales = this.getApplicationLocales();
			
			for (Locale locale : locales)
			{
				ResourceBundle bundle = ResourceBundle.getBundle("messages", locale);
				this.deployI18nJavascriptFile(bundle);
			}
		} catch (Exception e) {
			logger.error("Could not deploy the Javascript files with error message " + e.getMessage());
			e.printStackTrace();
		}
	}
}
