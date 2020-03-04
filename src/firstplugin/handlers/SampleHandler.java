package firstplugin.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import firstplugin.MethodVisitor;

public class SampleHandler extends AbstractHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		IProject[] projects = root.getProjects();
		for(IProject project:projects) {
			if(project.isOpen()) {
				try {
					IPackageFragment[] packages=JavaCore.create(project).getPackageFragments();
					for(IPackageFragment p:packages) {
						if(p.getKind()==IPackageFragmentRoot.K_SOURCE) {
							createAST(p);
						}
					}
				} catch (JavaModelException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}
	
	private void createAST(IPackageFragment mypackage) throws JavaModelException {
        for (ICompilationUnit unit : mypackage.getCompilationUnits()) {
            CompilationUnit parse = parse(unit);
            MethodVisitor visitor = new MethodVisitor();
            parse.accept(visitor);

            for (MethodDeclaration method : visitor.getMethods()) {
                System.out.println("Method name: " + method.getName()
                        + " Return type: " + method.getReturnType2());
            }
        }
    }
	
	private static CompilationUnit parse(ICompilationUnit unit) {
        ASTParser parser = ASTParser.newParser(AST.JLS10);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(unit);
        parser.setResolveBindings(true);
        return (CompilationUnit) parser.createAST(null); // parse
    }
}
