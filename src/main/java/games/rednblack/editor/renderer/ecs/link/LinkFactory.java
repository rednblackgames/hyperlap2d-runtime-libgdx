package games.rednblack.editor.renderer.ecs.link;

import games.rednblack.editor.renderer.ecs.ComponentType;
import games.rednblack.editor.renderer.ecs.Entity;
import games.rednblack.editor.renderer.ecs.Engine;
import games.rednblack.editor.renderer.ecs.annotations.EntityId;
import games.rednblack.editor.renderer.ecs.annotations.LinkPolicy;
import games.rednblack.editor.renderer.ecs.utils.Bag;
import games.rednblack.editor.renderer.ecs.utils.IntBag;
import com.badlogic.gdx.utils.reflect.Annotation;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Field;

import static games.rednblack.editor.renderer.ecs.annotations.LinkPolicy.Policy.SKIP;
import static games.rednblack.editor.renderer.ecs.utils.reflect.ReflectionUtil.isGenericType;

class LinkFactory {
	private static final int NULL_REFERENCE = 0;
	private static final int SINGLE_REFERENCE = 1;
	private static final int MULTI_REFERENCE = 2;

	private final Bag<LinkSite> links = new Bag<LinkSite>();
	private final Engine engine;

	private final ReflexiveMutators reflexiveMutators;

	public LinkFactory(Engine engine) {
		this.engine = engine;
		reflexiveMutators = new ReflexiveMutators(engine);
	}

	static int getReferenceTypeId(Field f) {
		Class type = f.getType();
		if (Entity.class == type)
			return SINGLE_REFERENCE;
		if (isGenericType(f, Bag.class, Entity.class))
			return MULTI_REFERENCE;

		boolean explicitEntityId = f.getDeclaredAnnotation(EntityId.class) != null;
		if (int.class == type && explicitEntityId)
			return SINGLE_REFERENCE;
		if (IntBag.class == type && explicitEntityId)
			return MULTI_REFERENCE;

		return NULL_REFERENCE;
	}

	Bag<LinkSite> create(ComponentType ct) {
		Class<?> type = ct.getType();
		Field[] fields = ClassReflection.getDeclaredFields(type);

		links.clear();
		for (int i = 0; fields.length > i; i++) {
			Field f = fields[i];
			int referenceTypeId = getReferenceTypeId(f);
			if (referenceTypeId != NULL_REFERENCE && (SKIP != getPolicy(f))) {
				if (SINGLE_REFERENCE == referenceTypeId) {
					UniLinkSite ls = new UniLinkSite(engine, ct, f);
					if (!configureMutator(ls))
						reflexiveMutators.withMutator(ls);

					links.add(ls);
				} else if (MULTI_REFERENCE == referenceTypeId) {
					MultiLinkSite ls = new MultiLinkSite(engine, ct, f);
					if (!configureMutator(ls))
						reflexiveMutators.withMutator(ls);

					links.add(ls);
				}
			}
		}

		return links;
	}

	static LinkPolicy.Policy getPolicy(Field f) {
		Annotation annotation = f.getDeclaredAnnotation(LinkPolicy.class);
		if (annotation != null) {
			LinkPolicy lp = annotation.getAnnotation(LinkPolicy.class);
			return lp != null ? lp.value() : null;
		}

		return null;
	}

	private boolean configureMutator(UniLinkSite linkSite) {
		UniFieldMutator mutator = MutatorUtil.getGeneratedMutator(linkSite);
		if (mutator != null) {
			mutator.setEngine(engine);
			linkSite.fieldMutator = mutator;
			return true;
		} else {
			return false;
		}
	}

	private boolean configureMutator(MultiLinkSite linkSite) {
		MultiFieldMutator mutator = MutatorUtil.getGeneratedMutator(linkSite);
		if (mutator != null) {
			mutator.setEngine(engine);
			linkSite.fieldMutator = mutator;
			return true;
		} else {
			return false;
		}
	}

	static class ReflexiveMutators {
		final EntityFieldMutator entityField;
		final IntFieldMutator intField;
		final IntBagFieldMutator intBagField;
		final EntityBagFieldMutator entityBagField;

		public ReflexiveMutators(Engine engine) {
			entityField = new EntityFieldMutator();
			entityField.setEngine(engine);

			intField = new IntFieldMutator();
			intField.setEngine(engine);

			intBagField = new IntBagFieldMutator();
			intBagField.setEngine(engine);

			entityBagField = new EntityBagFieldMutator();
			entityBagField.setEngine(engine);
		}

		UniLinkSite withMutator(UniLinkSite linkSite) {
			if (linkSite.fieldMutator != null)
				return linkSite;

			Class type = linkSite.field.getType();
			if (Entity.class == type) {
				linkSite.fieldMutator = entityField;
			} else if (int.class == type) {
				linkSite.fieldMutator = intField;
			} else {
				throw new RuntimeException("unexpected '" + type + "', on " + linkSite.type);
			}

			return linkSite;
		}

		MultiLinkSite withMutator(MultiLinkSite linkSite) {
			if (linkSite.fieldMutator != null)
				return linkSite;

			Class type = linkSite.field.getType();
			if (IntBag.class == type) {
				linkSite.fieldMutator = intBagField;
			} else if (Bag.class == type) {
				linkSite.fieldMutator = entityBagField;
			} else {
				throw new RuntimeException("unexpected '" + type + "', on " + linkSite.type);
			}

			return linkSite;
		}
	}
}
