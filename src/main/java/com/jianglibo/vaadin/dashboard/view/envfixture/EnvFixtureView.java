package com.jianglibo.vaadin.dashboard.view.envfixture;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.jianglibo.vaadin.dashboard.domain.Box;
import com.jianglibo.vaadin.dashboard.domain.BoxGroup;
import com.jianglibo.vaadin.dashboard.domain.Person;
import com.jianglibo.vaadin.dashboard.domain.Software;
import com.jianglibo.vaadin.dashboard.init.AppInitializer;
import com.jianglibo.vaadin.dashboard.repositories.BoxGroupRepository;
import com.jianglibo.vaadin.dashboard.repositories.BoxRepository;
import com.jianglibo.vaadin.dashboard.repositories.PersonRepository;
import com.jianglibo.vaadin.dashboard.repositories.SoftwareRepository;
import com.jianglibo.vaadin.dashboard.security.M3958SecurityUtil;
import com.jianglibo.vaadin.dashboard.security.PersonVo;
import com.jianglibo.vaadin.dashboard.service.AppObjectMappers;
import com.jianglibo.vaadin.dashboard.sshrunner.EnvForCodeExec;
import com.jianglibo.vaadin.dashboard.taskrunner.OneThreadTaskDesc;
import com.jianglibo.vaadin.dashboard.taskrunner.TaskDesc;
import com.jianglibo.vaadin.dashboard.util.ListViewFragmentBuilder;
import com.jianglibo.vaadin.dashboard.util.MsgUtil;
import com.jianglibo.vaadin.dashboard.util.NotificationUtil;
import com.jianglibo.vaadin.dashboard.util.SoftwareFolder;
import com.jianglibo.vaadin.dashboard.util.StyleUtil;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Responsive;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@SuppressWarnings("serial")
@SpringView(name = EnvFixtureView.VIEW_NAME)
public class EnvFixtureView extends VerticalLayout implements View {

	private static final Logger LOGGER = LoggerFactory.getLogger(EnvFixtureView.class);

	private final MessageSource messageSource;

	public final static String VIEW_NAME = "envfixture";

	public static final FontAwesome ICON_VALUE = FontAwesome.WRENCH;

	private ListViewFragmentBuilder lvfb;
	
	private final AppObjectMappers appObjectMappers;
	
	private final BoxGroupRepository boxGroupRepository;
	
	private final BoxRepository boxRepository;
	
	private final PersonRepository personRepository;
	
	private final SoftwareRepository softwareRepository;
	
	private Button backBtn;

	private Label title;

	@Autowired
	public EnvFixtureView(MessageSource messageSource, AppObjectMappers appObjectMappers,SoftwareRepository softwareRepository, BoxGroupRepository boxGroupRepository, BoxRepository boxRepository, PersonRepository personRepository) {
		this.messageSource = messageSource;
		this.appObjectMappers = appObjectMappers;
		this.boxGroupRepository = boxGroupRepository;
		this.boxRepository = boxRepository;
		this.personRepository = personRepository;
		this.softwareRepository = softwareRepository;
		setSizeFull();
		addStyleName("transactions");
		addComponent(createTop());

		VerticalLayout vl = new VerticalLayout();
		vl.setSpacing(true);
		vl.setSizeFull();
		Component tb = toolbars();
		vl.addComponent(tb);

		addComponent(vl);
		setExpandRatio(vl, 1);
	}

	private Component toolbars() {
		CssLayout vl = new CssLayout();
		vl.setSizeFull();
		Responsive.makeResponsive(vl);
		StyleUtil.setMarginTwenty(vl);

		TextField filePathField = new TextField();

		Button filePathBtn = new Button(MsgUtil.getMsgFallbackToSelf(messageSource, "view.envfixture.", "filePathBtn"),
				event -> {
					if (!Strings.isNullOrEmpty(filePathField.getValue())) {
						Path scriptPaht = Paths.get(filePathField.getValue());
						Person person = personRepository.findByEmail(AppInitializer.firstEmail);
						
						if (Files.exists(scriptPaht) && Files.isDirectory(scriptPaht) && scriptPaht.isAbsolute()){
							try (Stream<Path> pathstream = Files.walk(scriptPaht)) {
								pathstream.filter(p -> SoftwareFolder.descriptionyml.equals(p.getFileName().toString())).filter(p -> {
									Path sp = p.getParent().getParent().resolve("sample-env");
									return Files.exists(sp) && Files.exists(sp.resolve("boxgroup.yaml"));
								}).map(SoftwareFolder::new).map(sfolder -> {
									try {
										BoxGroup bg = appObjectMappers.getYmlObjectMapper().readValue(sfolder.readBoxgroupYaml(), BoxGroup.class);
										bg.setConfigContent(sfolder.readBoxgroupConfigContent());
										BoxGroup bgInDb = boxGroupRepository.findByName(bg.getName());

										if (bgInDb != null) {
											bgInDb.getBoxes().forEach(b -> {
												b.getBoxGroups().remove(bgInDb);
												boxRepository.save(b);
											});
											boxGroupRepository.delete(bgInDb);
										}

										bg.setCreator(person);

										bg = boxGroupRepository.save(bg);

										final Set<BoxGroup> bgs = Sets.newHashSet(bg);

										Set<Box> boxes = bg.getBoxes().stream().map(box -> {
											Box boxInDb = boxRepository.findByIp(box.getIp());
											if (boxInDb != null) {
												boxRepository.delete(boxInDb);
											}
											box.setCreator(person);
											box.setBoxGroups(bgs);
											return boxRepository.save(box);
										}).collect(Collectors.toSet());

										bg.setBoxes(boxes);
										bgs.add(boxGroupRepository.save(bg));
										sfolder.setBoxGroup(bg);
										return sfolder;
									} catch (Exception e) {
										e.printStackTrace();
									}
									return null;
								})
								.map(sfolder -> {
									try {
										Software sf = appObjectMappers.getYmlObjectMapper().readValue(sfolder.readDescriptionyml(), Software.class);
										sf.setConfigContent(sfolder.getSoftwareConfigContent(sf.getConfigContent()));
										sfolder.setSoftware(sf);
										return (SoftwareFolder)sfolder;
									} catch (Exception e) {
										e.printStackTrace();
										return null;
									}
								}).map(sfolder -> {
									try {
										Software sf = sfolder.getSoftware();
										Software sfInDb = softwareRepository.findByNameAndOstypeAndSversion(sf.getName(), sf.getOstype(),
												sf.getSversion());
										if (sfInDb != null) {
											softwareRepository.delete(sfInDb);
										}
										sf.setCreator(person);
										softwareRepository.save(sf);
										
										TaskDesc td = new TaskDesc("", new PersonVo.PersonVoBuilder(person).build(), sfolder.getBoxGroup(),
												Sets.newHashSet(), sf, "install");

										OneThreadTaskDesc ottd = td.createOneThreadTaskDescs().get(0);

										EnvForCodeExec efce = new EnvForCodeExec.EnvForCodeExecBuilder(appObjectMappers, ottd,
												"/opt/easyinstaller").build();
										
										Path testFolder = sfolder.getTestPath();
										
										switch (sf.getPreferredFormat()) {
										case "JSON":
											String json = appObjectMappers.getObjectMapperNoIdent().writeValueAsString(efce);
											Files.write(testFolder.resolve("envforcodeexec.json"), json.getBytes());
											break;
										case "YAML":
											String yml = appObjectMappers.getYmlObjectMapper().writeValueAsString(efce);
											Files.write(testFolder.resolve("envforcodeexec.yaml"), yml.getBytes());
											break;
										case "XML":
											String xml = appObjectMappers.getXmlObjectMapper().writeValueAsString(efce);
											Files.write(testFolder.resolve("envforcodeexec.xml"), xml.getBytes());
											break;
										default:
											break;
										}
									} catch (Exception e) {
										e.printStackTrace();
									}
									return 1;
								}).count();
							return;
						} catch (Exception e2) {

						}
					}
				}
					NotificationUtil.tray(messageSource, "illegalScriptFolder", filePathField.getValue());
				});

		StyleUtil.setMarginLeftTen(filePathBtn);

		Label descriptionLabel = new Label();
		descriptionLabel.setContentMode(ContentMode.HTML);
		descriptionLabel
				.setValue(MsgUtil.getMsgFallbackToSelf(messageSource, "view.envfixture.", "descriptionLabel"));
		vl.addComponents(filePathField, filePathBtn, descriptionLabel);
		return vl;
	}

	@Override
	public void enter(ViewChangeEvent event) {
		setLvfb(new ListViewFragmentBuilder(event));
		if (getLvfb().getPreviousView().isPresent()) {
			StyleUtil.show(backBtn);
		}
	}

	private Component createTop() {
		HorizontalLayout hl = new HorizontalLayout();

		hl.addStyleName("viewheader");
		hl.setSpacing(true);
		Responsive.makeResponsive(hl);

		title = new Label(MsgUtil.getMsgFallbackToSelf(messageSource, "view.envfixture.", "title"));
		title.setSizeUndefined();
		title.addStyleName(ValoTheme.LABEL_H1);
		title.addStyleName(ValoTheme.LABEL_NO_MARGIN);
		hl.addComponent(title);

		HorizontalLayout tools = new HorizontalLayout();
		tools.addStyleName("toolbar");
		hl.addComponent(tools);

		backBtn = new Button(FontAwesome.MAIL_REPLY);
		StyleUtil.hide(backBtn);

		backBtn.setDescription(MsgUtil.getMsgWithSubsReturnKeyOnAbsent(messageSource, "shared.btn.return"));

		backBtn.addClickListener(event -> {
			this.backward();
		});
		tools.addComponent(backBtn);
		return hl;
	}

	public void backward() {
		UI.getCurrent().getNavigator().navigateTo(getLvfb().getPreviousView().get());
	}

	public ListViewFragmentBuilder getLvfb() {
		return lvfb;
	}

	public void setLvfb(ListViewFragmentBuilder lvfb) {
		this.lvfb = lvfb;
	}
}
