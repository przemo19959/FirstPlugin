package firstplugin.handlers;

import java.lang.annotation.Annotation;
import java.util.Arrays;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.LineComment;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.*;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.TextEdit;

import firstplugin.MethodVisitor;

public class SampleHandler extends AbstractHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		IProject[] projects = root.getProjects();
		for (IProject project : projects) {
			if (project.isOpen()) {
				try {
					IPackageFragment[] packages = JavaCore.create(project).getPackageFragments();
					for (IPackageFragment p : packages) {
						if (p.getKind() == IPackageFragmentRoot.K_SOURCE) {
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
				CompilationUnit sourceFile = parse(unit);
				MethodVisitor visitor = new MethodVisitor();
				sourceFile.accept(visitor);

//			System.out.println("typy" + unit.getElementName());

//			for (MethodDeclaration method : visitor.getMethods()) {
//				System.out.println("Method name: " + method.getName() + " Return type: " + method.getReturnType2());
//			}
				boolean fieldAlreadyExists=false;
				boolean annotationPresent=false;
				for (TypeDeclaration type : visitor.getTypes()) {
//				System.out.println("Type: "+type);
					AST typeAST = type.getAST();
					for(Object modifier:type.modifiers()) {
						if(modifier instanceof MarkerAnnotation) {
							MarkerAnnotation ma=(MarkerAnnotation) modifier;
							if(ma.getTypeName().getFullyQualifiedName().equals("Singleton")) {
								annotationPresent=true;
								break;
							}
								
						}
					}
					if(annotationPresent==false)
						continue;
					System.out.println(type);					
					
					for(FieldDeclaration fd:type.getFields()) {
						//important to add newline 
						if(fd.toString().endsWith("INSTANCE;\n")) {
							fieldAlreadyExists=true;
							break;
						}
					}
					if(fieldAlreadyExists) {
						fieldAlreadyExists=false;
						continue;
					}
//            	private volatile static Dao INSTANCE;
					VariableDeclarationFragment vdf = typeAST.newVariableDeclarationFragment();
					vdf.setName(typeAST.newSimpleName("INSTANCE"));

					FieldDeclaration fd = typeAST.newFieldDeclaration(vdf);
					FieldAccess fa = typeAST.newFieldAccess();
					fd.modifiers().addAll(typeAST.newModifiers(Flags.AccPrivate | Flags.AccVolatile | Flags.AccStatic));
					fd.setType(typeAST.newSimpleType(typeAST.newName(type.getName().getFullyQualifiedName())));
					
					LineComment lc=typeAST.newLineComment();
					//TODO 5 mar 2020:spróbować jakoś dodać tekst do komentarza

					ASTRewrite r = ASTRewrite.create(typeAST);
					ListRewrite listRewrite = r.getListRewrite(type, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
					listRewrite.insertFirst(fd, null);
					try {
						save(sourceFile, r);
					} catch (CoreException e) {
						e.printStackTrace();
					}
				}

//			for (AnnotationTypeDeclaration a : visitor.getAnnotations()) {
//				System.out.println("Annotation: " + a);
//				
//			}
		}
	}

	private void save(CompilationUnit unit, ASTRewrite rewrite) throws CoreException {
		ITextFileBufferManager bufferManager = FileBuffers.getTextFileBufferManager();
		IPath path = unit.getJavaElement().getPath();
		try {
			bufferManager.connect(path, null);
			ITextFileBuffer textFileBuffer = bufferManager.getTextFileBuffer(path);
			IDocument document = textFileBuffer.getDocument();
			TextEdit edit = rewrite.rewriteAST(document, null);
			edit.apply(document);
			textFileBuffer.commit(null /* ProgressMonitor */, true /* Overwrite */);
		} catch (Exception e) {
			System.out.println(e);
		} finally {
			bufferManager.disconnect(path, null);
		}
	}

	private static CompilationUnit parse(ICompilationUnit unit) {
		ASTParser parser = ASTParser.newParser(AST.JLS12);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(unit);
		parser.setResolveBindings(true);
		return (CompilationUnit) parser.createAST(null); // parse
	}
}
