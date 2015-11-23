import javax.swing.*;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class Cleaner {

	public static final String TITLE_APP = "OC module cleaner";
	public static final String ADMIN_DIR = "admin";
	public static final String CATALOG_DIR = "catalog";
	public static final String BLANK = "";
	public static final String BTN_OC_PATH_CHOOSER = "Выбрать";
	public static final String BTN_RM_MODULE = "Убрать модуль";
	public static final String MSG_JL_CONSOLE_PROMPT = "Перетащите в это окно модуль: ";
	public static final String MSG_TITLE_JFCH = "Выберите путь к /root каталогу opencart";
	public static final String MSG_JTF_OC_PATH = "Путь к /root каталогу opencart";
	public static final String MSG_ENTER_OC_PATH = "Введите путь к корню (/root) магазина";
	public static final String MSG_REMAINING_FILES = "Оставшиеся файлы";
	public static final String MSG_THIS_NOT_DIR = "Это не каталог !";
	public static final String MSG_WARNING = "Предупреждение";
	public static final String MSG_THIS_NOT_OC_MODULE = "Это не opencart модуль !";
	public static final String MSG_ERROR = "Ошибка";
	public static final String MSG_SEL_MORE_ONE_ELEM = "Выделено более одного элемента, нужно выделеть только один элемент - модуль.";
	public static final String MSG_MOD_EMPTY = "Модуль не содержит фалов !";

	JLabel jlConsolePromptMsg;
	JTextPane jtpConsole;
	JScrollPane jscpConsole;
	JTextField jtfOCPath;
	JButton btnOCPathChooser;
	JButton btnRemoveModule;


	public Cleaner() {
		buildUI();
		addUiListeners();
	}

	/**
	 * Создание UI
	 */
	public void buildUI() {
		try {
			UIManager.setLookAndFeel(new NimbusLookAndFeel());
		} catch (UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}

		jlConsolePromptMsg = new JLabel(MSG_JL_CONSOLE_PROMPT);

		jtpConsole = new JTextPane();
		jtpConsole.setEditable(false);
		jscpConsole = new JScrollPane();
		jscpConsole.setViewportView(jtpConsole);
		jscpConsole.setPreferredSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));

		jtfOCPath = new JTextField();
		jtfOCPath.setPreferredSize(new Dimension());
		jtfOCPath.setToolTipText(MSG_JTF_OC_PATH);
		jtfOCPath.setText("/home/max/pcshopoc156-copy");

		btnOCPathChooser = new JButton(BTN_OC_PATH_CHOOSER);
		btnRemoveModule = new JButton(BTN_RM_MODULE);

		JPanel jpPath = new JPanel();
		jpPath.setLayout(new BorderLayout());
		jpPath.add(jtfOCPath, BorderLayout.CENTER);
		jpPath.add(btnOCPathChooser, BorderLayout.LINE_END);

		JPanel jpMain = new JPanel();
		jpMain.setLayout(new BoxLayout(jpMain, BoxLayout.Y_AXIS));
		jpMain.add(jlConsolePromptMsg);
		jpMain.add(jscpConsole);
		jpMain.add(jpPath);
		jpMain.add(btnRemoveModule);

		JFrame jfMain = new JFrame(TITLE_APP);
		jfMain.getContentPane().setLayout(new BorderLayout());
		jfMain.add(jpMain);
		jfMain.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		jfMain.setPreferredSize(new Dimension(500, 500));
		jfMain.pack();
		jfMain.setVisible(true);
	}

	/**
	 * Добавление слушателей к элементам UI
	 */
	private void addUiListeners() {
		btnOCPathChooser.addActionListener(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser jfch = new JFileChooser();
				jfch.setAcceptAllFileFilterUsed(true);
				jfch.setDialogTitle(MSG_TITLE_JFCH);
				jfch.setMultiSelectionEnabled(false);
				jfch.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				int answer = jfch.showOpenDialog(jfch);
				if (answer == JFileChooser.APPROVE_OPTION) {
					jtfOCPath.setText(jfch.getSelectedFile().toString());
				}
			}
		});

		btnRemoveModule.addActionListener(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (jtfOCPath.getText().equals(BLANK)) {
					JOptionPane.showMessageDialog(null, MSG_ENTER_OC_PATH);
					return;
				}
				List<String> doNotRmFiles = removeModFiles(jtfOCPath.getText(), relPathsModFiles);
				displayInform(MSG_REMAINING_FILES, doNotRmFiles, ViewInformActions.NEW);
			}
		});

		jtpConsole.setTransferHandler(new TransferHandler(){
			@Override
			public boolean canImport(TransferHandler.TransferSupport support) {
				if (!support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
					return false;
				}
				boolean copySupported = (COPY & support.getSourceDropActions()) == COPY;
				if (!copySupported) {
					return false;
				}
				support.setDropAction(COPY);
				return true;
			}

			@Override
			public boolean importData(TransferHandler.TransferSupport support) {
				if (!canImport(support)) {
					return false;
				}
				Transferable t = support.getTransferable();
				try {
					ArrayList<File> files = (ArrayList<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
					if (isOneElementSelected(files) && fileOrDirCheck(files) && isOpenCartModule(files)) {
						process(files);
					}
				} catch (UnsupportedFlavorException e) {
					return false;
				} catch (IOException e) {
					return false;
				}

				return true;
			}
		});
	}

	/**
	 * Проверка - в окно при помощи drag and drop должен быть перенесен только один элемент - модуль
	 * @param files список выделенных элементов (файлов или каталогов) и перемещенных в окно
	 * @return true выделен только одиин элемнт
	 */
	private boolean isOneElementSelected(ArrayList<File> files) {
		if (files.size() > 1) {
			JOptionPane.showMessageDialog(null, MSG_SEL_MORE_ONE_ELEM, MSG_WARNING, JOptionPane.WARNING_MESSAGE);
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Проверка, является ли полученный список файлом(файлами)\каталогом.
	 * Каталог, это тот же файл, но определеными свойствами.
	 * @param files список выделенных элементов (файлов или каталогов) и перемещенных в окно
	 */
	private boolean fileOrDirCheck(ArrayList<File> files){
		if (files.get(0).isDirectory()){
			return true;
		} else if (files.get(0).isFile()) {
			JOptionPane.showMessageDialog(null, MSG_THIS_NOT_DIR, MSG_WARNING, JOptionPane.WARNING_MESSAGE);
			return false;
		} else {
			return false;
		}
	}

	/**
	 * Проверка, являеться ли выбранный каталог модулем opencart
	 * @param files список выделенных элементов (файлов или каталогов) и перемещенных в окно
	 * @return true это opencart модуль
	 */
	private boolean isOpenCartModule(ArrayList<File> files) {
		File[] filesInDir = files.get(0).listFiles();
		if (filesInDir != null) {
			for (File f : filesInDir) {
				if (f.getName().equals(ADMIN_DIR) || f.getName().equals(CATALOG_DIR)) {
					return true;
				}
			}
			JOptionPane.showMessageDialog(null, MSG_THIS_NOT_OC_MODULE, MSG_ERROR, JOptionPane.ERROR_MESSAGE);
		}
		return false;
	}

	/* Найденные файлы модуля, абсолютные пути*/
	private List<String> absPathsModFiles = new ArrayList<>();
	/* Найденные файлы модуля, относительные пути*/
	private List<String> relPathsModFiles = new ArrayList<>();
	/* Количество фалов в каталоге этого модуля */
	private int countFiles = 0;
	/* Количчество каталогов в каталоге этого модуля */
	private int countDirs = 0;

	/**
	 * Процесс получение списка файлов модуля с относительными путями
	 * @param filesList список с внутренним содержимым корневого каталога магазина (файлы и каталоги)
	 */
	private void process(ArrayList<File> filesList) {
		clearModInform();
		File[] filesAndDirsInMod = filesList.get(0).listFiles();
		//todo - нужна ли эта проверка ?
		if (filesAndDirsInMod == null) {
			throw new NullPointerException("no files and dirs !");
		}
		Arrays.sort(filesAndDirsInMod); //сортировка по возрастанию
		String absPathToMod = filesList.get(0).getPath(); //абсолютный путь к каталогу модуля
		for (File file : filesAndDirsInMod) {
			getModuleFiles(file);
		}
		System.out.println("countFiles = " + countFiles);
		System.out.println("countDirs = " + countDirs);
		// получаем относительные пути к файлам модуля
		if (absPathsModFiles != null) {
			for (String path : absPathsModFiles) {
				relPathsModFiles.add(path.substring(absPathToMod.length(), path.length()));
			}
		}
		if (isEmptyModule()) return;

		displayInform(null, relPathsModFiles, ViewInformActions.NEW);
	}

	/**
	 * Рекурсивный поиск списка файлов в каталоге модуля
	 * @param path абсолютный путь у каталогам и файлам в модуле
	 */
	private void getModuleFiles(File path) {
		if (path.isFile()) {
			absPathsModFiles.add(path.getPath());
			countFiles ++;
		} else if (path.isDirectory()) {
			File files[] = path.listFiles();
			if (files != null) {
				for (File dirOrFile : files) {
					getModuleFiles(dirOrFile);
				}
			}
			countDirs ++;
		}
	}

	/**
	 * При каждом новом перетаскивании в окно показа файлов через d&d мы загружакм новый модуль.
	 * Соответсвенно информацтю о предыдущкм модуле (если он был загружен ранее) стираем.
	 * @return true коллекции хранящии информ. о модуле были очищены.
	 */
	private boolean clearModInform() {
		if (!absPathsModFiles.isEmpty() || !relPathsModFiles.isEmpty()) {
			absPathsModFiles.clear();
			relPathsModFiles.clear();
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Проверка модуля на пустоту. Если есть один из каталогов {@link #ADMIN_DIR} или {@link #CATALOG_DIR}
	 * но там нет никаких фалов модуль считаеться пустым.
	 * @return true если модуль пустой
	 */
	private boolean isEmptyModule() {
		if (relPathsModFiles.isEmpty()) {
			JOptionPane.showMessageDialog(null, MSG_MOD_EMPTY, MSG_WARNING, JOptionPane.WARNING_MESSAGE);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Выполнение удаления списка файлов на ФС
	 * @param ocRootAbsPath абсолютный путь к корневому каталогу opencart
	 * @param modFilesRelPaths список файлов для удаления
	 * @return список с файлами которые не удалось стереть с ФС. Возможно этих фалов просто нет на ФС.
	 */
	private List<String> removeModFiles(String ocRootAbsPath, List<String> modFilesRelPaths) {
		if (modFilesRelPaths.isEmpty()) { //todo - may be this check is not
			return null;
		}
		List<String> listNotRmFiles = new ArrayList<>();
		for (String path : modFilesRelPaths) {
			boolean isDelete = new File(ocRootAbsPath + path).delete();
			if (!isDelete) {
				listNotRmFiles.add(path);
			}
		}
		return listNotRmFiles;
	}

	/**
	 * Вывод информации в окне через которое мы загружам модуль через d&d.
	 * Сдесь возможен вывод списка файлов модуля, а также информации о стертых и не стертых фалов
	 * @param msg информационное сообщение
	 * @param relPathsToModFiles относитьельные пути к файлам модуля
	 * @param action действие при вывода информации в инф. окно {@see ViewInformActions}
	 */
	private void displayInform(String msg, List<String> relPathsToModFiles, ViewInformActions action) {
		String inform = BLANK;
		if (action == ViewInformActions.NEW) {
			jtpConsole.setText(BLANK);
		} else if (action == ViewInformActions.ADD) {
			inform = jtpConsole.getText();
		}

		if (msg != null && !msg.equals(BLANK)) {
			inform += msg + ": \n";
		}
		for (String modFile : relPathsToModFiles) {
			inform += modFile + "\n";
		}
		jtpConsole.setText(inform);
		jtpConsole.setForeground(new Color(40, 40, 40));
		jtpConsole.setSelectionColor(new Color(112, 112, 112));
		jtpConsole.setFont(new Font("Veranda", Font.BOLD, 12));
	}

	/**
	 * Возможные варианты дейсвия для вывода информации в инф. окне
	 * {@link Cleaner.ViewInformActions#NEW} - все предыдущие записи стираються
	 * {@link Cleaner.ViewInformActions#ADD} - добавление к существующему тексту нового текста
	 */
	private enum ViewInformActions {
		ADD,
		NEW
	}

}
