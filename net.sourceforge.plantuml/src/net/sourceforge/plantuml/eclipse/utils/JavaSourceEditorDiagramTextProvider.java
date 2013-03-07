package net.sourceforge.plantuml.eclipse.utils;


import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;

public class JavaSourceEditorDiagramTextProvider extends JavaEditorDiagramTextProvider{
	
	@Override
	protected String getDiagramText(IEditorPart editorPart, IEditorInput editorInput) {
		if (! (editorInput instanceof IFileEditorInput)) {
			return null;
		}
		currentContext = new Context();
		IPath path = ((IFileEditorInput) editorInput).getFile().getFullPath();
		currentContext.setProject(ResourcesPlugin.getWorkspace().getRoot().getProject(path.segment(0)));
		currentContext.setJavaProject(JavaCore.create(currentContext.getProject()));
		currentContext.setCompUnit(JavaCore.createCompilationUnitFrom(currentContext.getProject().getFile(path.removeFirstSegments(1))));
		StringBuilder result = new StringBuilder();
		try {
			currentContext.getCompUnit().open(new NullProgressMonitor());
			for (IType type: currentContext.getCompUnit().getTypes()) {
				generateForType(type, result, GEN_MEMBERS | GEN_MODIFIERS | GEN_SUPERCLASS | GEN_INTERFACES, null);
			}
		} catch (JavaModelException e) {
			System.err.println(e);
		} finally {
			currentContext = null;
		}
		return (result.length() > 0 ? result.toString() : null);
	}

}
