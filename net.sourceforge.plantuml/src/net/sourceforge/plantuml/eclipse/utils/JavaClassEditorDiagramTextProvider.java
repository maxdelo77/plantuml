package net.sourceforge.plantuml.eclipse.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.PackageFragmentRoot;
import org.eclipse.jdt.internal.core.SourceMapper;
import org.eclipse.jdt.internal.ui.javaeditor.IClassFileEditorInput;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.LibraryLocation;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;

public class JavaClassEditorDiagramTextProvider extends
		JavaEditorDiagramTextProvider {

	@Override
	protected String getDiagramText(IEditorPart editorPart,
			IEditorInput editorInput) {
		if (!(editorInput instanceof IClassFileEditorInput)) {
			return null;
		}

		StringBuilder result = new StringBuilder();

		currentContext = new Context();

		String className = ((IClassFileEditorInput) editorInput).getName()
				.replace(".class", "");
		String completeClassName = className + ".java";
		
		String packageName = ((IClassFileEditorInput) editorInput)
				.getToolTipText().replace("." + className, "");
		
		String filePath = packageName.replace(".", "/");

		IClassFile cf = (IClassFile) editorInput.getAdapter(IClassFile.class);

		IJavaElement javaElement = (IJavaElement) cf;
		while ((javaElement != null)
				&& (javaElement.getElementType() != IJavaElement.PACKAGE_FRAGMENT_ROOT)) {
			javaElement = javaElement.getParent();
		}

		IProject project = null;
		try {

			/* Getting Source file */
			String sourceRootPath = null;
			PackageFragmentRoot fragmentRoot = ((PackageFragmentRoot) javaElement);
			SourceMapper sourceMapper = fragmentRoot.getSourceMapper();
			
			char[] source = sourceMapper.findSource(filePath + "/" + completeClassName);
			
			
			/* Create project packageRoot package file */

			IProgressMonitor nullProgressMonitor = new NullProgressMonitor();
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			project = root.getProject("sources-project");
			project.create(nullProgressMonitor);
			project.open(nullProgressMonitor);

			IProjectDescription description = project.getDescription();
			description.setNatureIds(new String[] { JavaCore.NATURE_ID });
			project.setDescription(description, nullProgressMonitor);

			IJavaProject javaProject = JavaCore.create(project);

			IFolder binFolder = project.getFolder("bin");
			binFolder.create(false, true, nullProgressMonitor);
			javaProject.setOutputLocation(binFolder.getFullPath(),
					nullProgressMonitor);

			List<IClasspathEntry> entries = new ArrayList<IClasspathEntry>();
			IVMInstall vmInstall = JavaRuntime.getDefaultVMInstall();
			LibraryLocation[] locations = JavaRuntime
					.getLibraryLocations(vmInstall);
			for (LibraryLocation element : locations) {
				entries.add(JavaCore.newLibraryEntry(
						element.getSystemLibraryPath(), null, null));
			}
			// add libs to project class path
			javaProject.setRawClasspath(
					entries.toArray(new IClasspathEntry[entries.size()]), null);

			IFolder sourceFolder = project.getFolder("src");
			sourceFolder.create(false, true, null);

			IPackageFragmentRoot packageFragmentRoot = javaProject
					.getPackageFragmentRoot(sourceFolder);
			IClasspathEntry[] oldEntries = javaProject.getRawClasspath();
			IClasspathEntry[] newEntries = new IClasspathEntry[oldEntries.length + 1];
			System.arraycopy(oldEntries, 0, newEntries, 0, oldEntries.length);
			newEntries[oldEntries.length] = JavaCore
					.newSourceEntry(packageFragmentRoot.getPath());
			javaProject.setRawClasspath(newEntries, null);

			IPackageFragment pack = javaProject.getPackageFragmentRoot(
					sourceFolder).createPackageFragment(packageName, false,
					null);

			StringBuffer buffer = new StringBuffer();
			buffer.append("package " + pack.getElementName() + ";\n");
			buffer.append("\n");
			buffer.append(new String(source));

			ICompilationUnit cu = pack.createCompilationUnit(completeClassName,
					buffer.toString(), false, null);

			currentContext.setCompUnit(cu);

			currentContext.getCompUnit().open(new NullProgressMonitor());
			for (IType type : currentContext.getCompUnit().getTypes()) {
				generateForType(type, result, GEN_MEMBERS | GEN_MODIFIERS
						| GEN_SUPERCLASS | GEN_INTERFACES, null);
			}

		} catch (JavaModelException e) {
			System.err.println(e);
		} catch (CoreException ce) {
			System.err.println(ce);
		} finally {

			if (project.exists()) {
				try {
					project.delete(true, new NullProgressMonitor());
				} catch (CoreException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			currentContext = null;
		}
		return (result.length() > 0 ? result.toString() : null);
	}
}
