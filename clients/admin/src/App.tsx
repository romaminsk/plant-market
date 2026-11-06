import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import {
  archiveProduct,
  archiveVariant,
  createCategory,
  createIndividualPlant,
  createProduct,
  createStockBatch,
  createVariant,
  deleteCategory,
  fetchCategories,
  fetchIndividualPlants,
  fetchProducts,
  fetchStockBatches,
  fetchVariants,
  formatPrice,
  login,
  releaseStockBatch,
  transitionPlant,
  updateCategory,
  updateIndividualPlant,
  updateProduct,
  updateVariant,
} from './api';
import type { Category, IndividualPlant, Product, StockBatch, Variant } from './api';
import { PlantGalleryEditor } from './PlantGalleryEditor';
import { ClaimsPanel } from './ClaimsPanel';

const TOKEN_KEY = 'pm.admin.accessToken';
const CURRENCY = 'BYN';

type Tab = 'products' | 'categories' | 'plants' | 'variants' | 'stock' | 'claims';

function formValue(form: FormData, key: string): string {
  return String(form.get(key) ?? '').trim();
}

function formNumber(form: FormData, key: string): number {
  return Number(formValue(form, key));
}

export default function App() {
  const [email, setEmail] = useState('admin@plantmarket.local');
  const [password, setPassword] = useState('');
  const [accessToken, setAccessToken] = useState<string | null>(() =>
    localStorage.getItem(TOKEN_KEY),
  );
  const [tab, setTab] = useState<Tab>('products');
  const [products, setProducts] = useState<Product[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [plants, setPlants] = useState<IndividualPlant[]>([]);
  const [batches, setBatches] = useState<StockBatch[]>([]);
  const [variants, setVariants] = useState<Variant[]>([]);
  const [variantProductId, setVariantProductId] = useState('');
  const [stockProductId, setStockProductId] = useState('');
  const [stockVariants, setStockVariants] = useState<Variant[]>([]);
  const [editingProduct, setEditingProduct] = useState<Product | null>(null);
  const [editingCategory, setEditingCategory] = useState<Category | null>(null);
  const [editingVariant, setEditingVariant] = useState<Variant | null>(null);
  const [editingPlant, setEditingPlant] = useState<IndividualPlant | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [loggingIn, setLoggingIn] = useState(false);

  useEffect(() => {
    if (accessToken) {
      localStorage.setItem(TOKEN_KEY, accessToken);
    } else {
      localStorage.removeItem(TOKEN_KEY);
    }
  }, [accessToken]);

  useEffect(() => {
    if (!accessToken) {
      setProducts([]);
      setCategories([]);
      setPlants([]);
      setBatches([]);
      return;
    }
    let cancelled = false;
    setLoading(true);
    setError(null);
    Promise.all([
      fetchProducts(accessToken),
      fetchCategories(accessToken),
      fetchIndividualPlants(accessToken),
      fetchStockBatches(accessToken),
    ])
      .then(([loadedProducts, loadedCategories, loadedPlants, loadedBatches]) => {
        if (!cancelled) {
          setProducts(loadedProducts);
          setCategories(loadedCategories);
          setPlants(loadedPlants);
          setBatches(loadedBatches);
        }
      })
      .catch((cause: unknown) => {
        if (!cancelled) {
          setError(cause instanceof Error ? cause.message : 'Неизвестная ошибка');
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [accessToken]);

  useEffect(() => {
    if (!accessToken || !variantProductId) {
      setVariants([]);
      return;
    }
    let cancelled = false;
    fetchVariants(accessToken, variantProductId)
      .then((loaded) => {
        if (!cancelled) {
          setVariants(loaded);
        }
      })
      .catch((cause: unknown) => {
        if (!cancelled) {
          setError(cause instanceof Error ? cause.message : 'Неизвестная ошибка');
        }
      });
    return () => {
      cancelled = true;
    };
  }, [accessToken, variantProductId]);

  useEffect(() => {
    if (!accessToken || !stockProductId) {
      setStockVariants([]);
      return;
    }
    let cancelled = false;
    fetchVariants(accessToken, stockProductId)
      .then((loaded) => {
        if (!cancelled) {
          setStockVariants(loaded);
        }
      })
      .catch((cause: unknown) => {
        if (!cancelled) {
          setError(cause instanceof Error ? cause.message : 'Неизвестная ошибка');
        }
      });
    return () => {
      cancelled = true;
    };
  }, [accessToken, stockProductId]);

  async function refresh() {
    if (!accessToken) {
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const [loadedProducts, loadedCategories, loadedPlants, loadedBatches] = await Promise.all([
        fetchProducts(accessToken),
        fetchCategories(accessToken),
        fetchIndividualPlants(accessToken),
        fetchStockBatches(accessToken),
      ]);
      setProducts(loadedProducts);
      setCategories(loadedCategories);
      setPlants(loadedPlants);
      setBatches(loadedBatches);
      if (variantProductId) {
        setVariants(await fetchVariants(accessToken, variantProductId));
      }
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Неизвестная ошибка');
    } finally {
      setLoading(false);
    }
  }

  async function handleLogin(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLoggingIn(true);
    setError(null);
    try {
      const tokens = await login(email, password);
      setAccessToken(tokens.accessToken);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Неизвестная ошибка');
    } finally {
      setLoggingIn(false);
    }
  }

  async function run(action: () => Promise<unknown>, success: string, form?: HTMLFormElement) {
    setError(null);
    setMessage(null);
    try {
      await action();
      form?.reset();
      setMessage(success);
      await refresh();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Неизвестная ошибка');
    }
  }

  function logout() {
    setAccessToken(null);
    setPassword('');
    setError(null);
    setMessage(null);
  }

  if (!accessToken) {
    return (
      <main className="login-shell">
        <section className="card login-card">
          <p className="eyebrow">Plant Market · Operations</p>
          <h1>Админка</h1>
          <p className="muted">Вход через auth-service. JWT хранится в localStorage для E2E.</p>
          <form onSubmit={handleLogin}>
            <label>
              Email
              <input
                type="email"
                value={email}
                autoComplete="username"
                onChange={(event) => setEmail(event.target.value)}
                required
              />
            </label>
            <label>
              Пароль
              <input
                type="password"
                value={password}
                autoComplete="current-password"
                minLength={12}
                onChange={(event) => setPassword(event.target.value)}
                required
              />
            </label>
            <button type="submit" disabled={loggingIn}>
              {loggingIn ? 'Вход…' : 'Войти'}
            </button>
          </form>
          {error ? <p className="error">{error}</p> : null}
        </section>
      </main>
    );
  }

  return (
    <main className="app-shell">
      <header className="topbar">
        <div>
          <p className="eyebrow">Plant Market · Operations</p>
          <h1>Админка</h1>
        </div>
        <div className="actions">
          <button type="button" onClick={refresh} disabled={loading}>
            Обновить
          </button>
          <button type="button" className="secondary" onClick={logout}>
            Выйти
          </button>
        </div>
      </header>

      <nav className="tabs" aria-label="Разделы админки">
        {(
          [
            ['products', 'Продукты'],
            ['categories', 'Категории'],
            ['plants', 'Экземпляры'],
            ['variants', 'Варианты'],
            ['stock', 'Партии'],
            ['claims', 'Претензии'],
          ] as const
        ).map(([value, label]) => (
          <button
            key={value}
            type="button"
            className={tab === value ? 'tab active' : 'tab'}
            onClick={() => setTab(value)}
          >
            {label}
          </button>
        ))}
      </nav>

      {error ? <p className="error">{error}</p> : null}
      {message ? <p className="success">{message}</p> : null}

      {tab === 'claims' && accessToken ? <ClaimsPanel accessToken={accessToken} /> : null}

      {tab === 'products' ? (
        <section className="panel-grid">
          <div className="card panel">
            <h2>{editingProduct ? 'Редактировать продукт' : 'Новый продукт'}</h2>
            <form
              key={editingProduct?.id ?? 'new-product'}
              onSubmit={(event) => {
                event.preventDefault();
                const form = event.currentTarget;
                const data = new FormData(form);
                const body = {
                  sku: formValue(data, 'sku'),
                  name: formValue(data, 'name'),
                  slug: formValue(data, 'slug'),
                  description: formValue(data, 'description') || null,
                  categoryId: formValue(data, 'categoryId'),
                  priceMinor: Math.round(formNumber(data, 'price') * 100),
                  currency: CURRENCY,
                  status: formValue(data, 'status'),
                  careLevel: formValue(data, 'careLevel'),
                  light: formValue(data, 'light'),
                  petSafe: data.get('petSafe') === 'on',
                  variegated: data.get('variegated') === 'on',
                  size: formValue(data, 'size') || null,
                  careGuide: formValue(data, 'careGuide') || null,
                };
                void run(
                  async () => {
                    if (editingProduct) {
                      await updateProduct(accessToken, editingProduct.id, body);
                      setEditingProduct(null);
                    } else {
                      await createProduct(accessToken, body);
                    }
                  },
                  editingProduct ? 'Продукт обновлён' : 'Продукт создан',
                  form,
                );
              }}
            >
              <div className="form-row">
                <label>
                  SKU
                  <input name="sku" defaultValue={editingProduct?.sku ?? ''} required />
                </label>
                <label>
                  Название
                  <input name="name" defaultValue={editingProduct?.name ?? ''} required />
                </label>
              </div>
              <div className="form-row">
                <label>
                  Slug
                  <input
                    name="slug"
                    defaultValue={editingProduct?.slug ?? ''}
                    required
                    pattern="[a-z0-9-]+"
                  />
                </label>
                <label>
                  Категория
                  <select
                    name="categoryId"
                    defaultValue={editingProduct?.categoryId ?? ''}
                    required
                  >
                    <option value="">Выберите категорию</option>
                    {categories.map((category) => (
                      <option key={category.id} value={category.id}>
                        {category.name}
                      </option>
                    ))}
                  </select>
                </label>
              </div>
              <div className="form-row">
                <label>
                  Цена, BYN
                  <input
                    name="price"
                    type="number"
                    min="0"
                    step="0.01"
                    defaultValue={
                      editingProduct ? (editingProduct.priceMinor / 100).toString() : ''
                    }
                    required
                  />
                </label>
                <label>
                  Размер
                  <input name="size" defaultValue={editingProduct?.size ?? ''} />
                </label>
              </div>
              <div className="form-row">
                <label>
                  Статус
                  <select name="status" defaultValue={editingProduct?.status ?? 'PUBLISHED'}>
                    <option>DRAFT</option>
                    <option>PUBLISHED</option>
                    <option>ARCHIVED</option>
                  </select>
                </label>
                <label>
                  Свет
                  <select name="light" defaultValue={editingProduct?.light ?? 'MEDIUM'}>
                    <option>LOW</option>
                    <option>MEDIUM</option>
                    <option>BRIGHT</option>
                    <option>DIRECT</option>
                  </select>
                </label>
                <label>
                  Уход
                  <select name="careLevel" defaultValue={editingProduct?.careLevel ?? 'EASY'}>
                    <option>EASY</option>
                    <option>MEDIUM</option>
                    <option>HARD</option>
                  </select>
                </label>
              </div>
              <label>
                Описание
                <textarea
                  name="description"
                  rows={2}
                  defaultValue={editingProduct?.description ?? ''}
                />
              </label>
              <label>
                Care guide
                <textarea
                  name="careGuide"
                  rows={2}
                  defaultValue={editingProduct?.careGuide ?? ''}
                />
              </label>
              <div className="checks">
                <label>
                  <input
                    type="checkbox"
                    name="variegated"
                    defaultChecked={editingProduct?.variegated}
                  />{' '}
                  Вариегатный
                </label>
                <label>
                  <input type="checkbox" name="petSafe" defaultChecked={editingProduct?.petSafe} />{' '}
                  Безопасен для питомцев
                </label>
              </div>
              <button type="submit">
                {editingProduct ? 'Сохранить продукт' : 'Создать продукт'}
              </button>
              {editingProduct ? (
                <button type="button" className="secondary" onClick={() => setEditingProduct(null)}>
                  Отмена
                </button>
              ) : null}
            </form>
          </div>
          <div className="card panel">
            <h2>Продукты</h2>
            <table>
              <thead>
                <tr>
                  <th>SKU</th>
                  <th>Название</th>
                  <th>Цена</th>
                  <th>Статус</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {products.map((product) => (
                  <tr key={product.id}>
                    <td className="mono">{product.sku}</td>
                    <td>{product.name}</td>
                    <td>{formatPrice(product.priceMinor, product.currency)}</td>
                    <td>
                      <span className={`badge badge-${product.status.toLowerCase()}`}>
                        {product.status}
                      </span>
                    </td>
                    <td>
                      <button type="button" onClick={() => setEditingProduct(product)}>
                        Редактировать
                      </button>
                      {product.status !== 'ARCHIVED' ? (
                        <button
                          type="button"
                          onClick={() =>
                            void run(
                              () => archiveProduct(accessToken, product.id),
                              'Продукт архивирован',
                            )
                          }
                        >
                          Архивировать
                        </button>
                      ) : null}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      ) : null}

      {tab === 'categories' ? (
        <section className="panel-grid">
          <div className="card panel">
            <h2>{editingCategory ? 'Редактировать категорию' : 'Новая категория'}</h2>
            <form
              key={editingCategory?.id ?? 'new-category'}
              onSubmit={(event) => {
                event.preventDefault();
                const form = event.currentTarget;
                const data = new FormData(form);
                const body = {
                  name: formValue(data, 'name'),
                  slug: formValue(data, 'slug'),
                  description: formValue(data, 'description') || null,
                };
                void run(
                  async () => {
                    if (editingCategory) {
                      await updateCategory(accessToken, editingCategory.id, body);
                      setEditingCategory(null);
                    } else {
                      await createCategory(accessToken, body);
                    }
                  },
                  editingCategory ? 'Категория обновлена' : 'Категория создана',
                  form,
                );
              }}
            >
              <label>
                Название
                <input name="name" defaultValue={editingCategory?.name ?? ''} required />
              </label>
              <label>
                Slug
                <input
                  name="slug"
                  defaultValue={editingCategory?.slug ?? ''}
                  required
                  pattern="[a-z0-9-]+"
                />
              </label>
              <label>
                Описание
                <textarea
                  name="description"
                  rows={2}
                  defaultValue={editingCategory?.description ?? ''}
                />
              </label>
              <button type="submit">{editingCategory ? 'Сохранить категорию' : 'Создать'}</button>
              {editingCategory ? (
                <button
                  type="button"
                  className="secondary"
                  onClick={() => setEditingCategory(null)}
                >
                  Отмена
                </button>
              ) : null}
            </form>
          </div>
          <div className="card panel">
            <h2>Категории</h2>
            <table>
              <thead>
                <tr>
                  <th>Slug</th>
                  <th>Название</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {categories.map((category) => (
                  <tr key={category.id}>
                    <td className="mono">{category.slug}</td>
                    <td>{category.name}</td>
                    <td>
                      <button type="button" onClick={() => setEditingCategory(category)}>
                        Редактировать
                      </button>
                      <button
                        type="button"
                        onClick={() =>
                          void run(
                            () => deleteCategory(accessToken, category.id),
                            'Категория удалена',
                          )
                        }
                      >
                        Удалить
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      ) : null}

      {tab === 'plants' ? (
        <section className="panel-grid">
          <div className="card panel">
            <h2>{editingPlant ? 'Редактировать экземпляр' : 'Новый экземпляр'}</h2>
            <form
              key={editingPlant?.id ?? 'new-plant'}
              onSubmit={(event) => {
                event.preventDefault();
                const form = event.currentTarget;
                const data = new FormData(form);
                const productId = formValue(data, 'productId');
                const body = {
                  code: formValue(data, 'code'),
                  description: formValue(data, 'description') || null,
                  photoUrl: formValue(data, 'photoUrl') || null,
                  priceMinor: Math.round(formNumber(data, 'price') * 100),
                  currency: CURRENCY,
                  careLevel: formValue(data, 'careLevel'),
                  light: formValue(data, 'light'),
                  petSafe: data.get('petSafe') === 'on',
                };
                void run(
                  async () => {
                    if (editingPlant) {
                      await updateIndividualPlant(accessToken, editingPlant.id, body);
                      setEditingPlant(null);
                    } else {
                      await createIndividualPlant(accessToken, productId, body);
                    }
                  },
                  editingPlant ? 'Экземпляр обновлён' : 'Экземпляр создан (ACQUIRED)',
                  form,
                );
              }}
            >
              <label>
                Продукт
                <select
                  name="productId"
                  required
                  defaultValue={editingPlant?.productId ?? ''}
                  disabled={Boolean(editingPlant)}
                >
                  <option value="">Выберите продукт</option>
                  {products.map((product) => (
                    <option key={product.id} value={product.id}>
                      {product.name}
                    </option>
                  ))}
                </select>
              </label>
              <div className="form-row">
                <label>
                  Код
                  <input name="code" defaultValue={editingPlant?.code ?? ''} required />
                </label>
                <label>
                  Цена, BYN
                  <input
                    name="price"
                    type="number"
                    min="0"
                    step="0.01"
                    defaultValue={editingPlant ? (editingPlant.priceMinor / 100).toString() : ''}
                    required
                  />
                </label>
              </div>
              <div className="form-row">
                <label>
                  Свет
                  <select name="light" defaultValue={editingPlant?.light ?? 'MEDIUM'}>
                    <option>LOW</option>
                    <option>MEDIUM</option>
                    <option>BRIGHT</option>
                    <option>DIRECT</option>
                  </select>
                </label>
                <label>
                  Уход
                  <select name="careLevel" defaultValue={editingPlant?.careLevel ?? 'EASY'}>
                    <option>EASY</option>
                    <option>MEDIUM</option>
                    <option>HARD</option>
                  </select>
                </label>
              </div>
              <label>
                Описание
                <textarea
                  name="description"
                  rows={2}
                  defaultValue={editingPlant?.description ?? ''}
                />
              </label>
              <label>
                Фото URL
                <input name="photoUrl" defaultValue={editingPlant?.photoUrl ?? ''} />
              </label>
              <label>
                <input type="checkbox" name="petSafe" defaultChecked={editingPlant?.petSafe} />{' '}
                Безопасен для питомцев
              </label>
              <button type="submit">
                {editingPlant ? 'Сохранить экземпляр' : 'Создать экземпляр'}
              </button>
              {editingPlant ? (
                <button type="button" className="secondary" onClick={() => setEditingPlant(null)}>
                  Отмена
                </button>
              ) : null}
            </form>
          </div>
          {editingPlant && accessToken ? (
            <PlantGalleryEditor
              accessToken={accessToken}
              plant={editingPlant}
              onSaved={(updated) => {
                setEditingPlant(updated);
                setPlants((previous) =>
                  previous.map((plant) => (plant.id === updated.id ? updated : plant)),
                );
              }}
            />
          ) : null}
          <div className="card panel">
            <h2>Экземпляры</h2>
            <table>
              <thead>
                <tr>
                  <th>Код</th>
                  <th>Продукт</th>
                  <th>Цена</th>
                  <th>Статус</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {plants.map((plant) => (
                  <tr key={plant.id}>
                    <td className="mono">{plant.code}</td>
                    <td>{plant.productName}</td>
                    <td>{formatPrice(plant.priceMinor, plant.currency)}</td>
                    <td>{plant.status}</td>
                    <td>
                      <button type="button" onClick={() => setEditingPlant(plant)}>
                        Редактировать
                      </button>
                      {plant.status === 'ACQUIRED' ? (
                        <button
                          type="button"
                          onClick={() =>
                            void run(
                              () => transitionPlant(accessToken, plant.id, 'QUARANTINE'),
                              'Переведён в карантин',
                            )
                          }
                        >
                          В карантин
                        </button>
                      ) : null}
                      {plant.status === 'QUARANTINE' ? (
                        <button
                          type="button"
                          onClick={() =>
                            void run(
                              () => transitionPlant(accessToken, plant.id, 'AVAILABLE'),
                              'Опубликован',
                            )
                          }
                        >
                          Опубликовать
                        </button>
                      ) : null}
                      {plant.status !== 'MORTALITY' && plant.status !== 'DELIVERED' ? (
                        <button
                          type="button"
                          onClick={() =>
                            void run(
                              () => transitionPlant(accessToken, plant.id, 'MORTALITY'),
                              'Экземпляр списан',
                            )
                          }
                        >
                          Списать
                        </button>
                      ) : null}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      ) : null}

      {tab === 'variants' ? (
        <section className="panel-grid">
          <div className="card panel">
            <h2>{editingVariant ? 'Редактировать вариант' : 'Новый вариант'}</h2>
            <form
              key={editingVariant?.id ?? 'new-variant'}
              onSubmit={(event) => {
                event.preventDefault();
                const form = event.currentTarget;
                const data = new FormData(form);
                const productId = formValue(data, 'productId');
                const body = {
                  sku: formValue(data, 'sku'),
                  name: formValue(data, 'name'),
                  size: formValue(data, 'size') || null,
                  potSize: formValue(data, 'potSize') || null,
                  form: formValue(data, 'form') || null,
                  priceMinor: Math.round(formNumber(data, 'price') * 100),
                  currency: CURRENCY,
                  status: formValue(data, 'status'),
                };
                void run(
                  async () => {
                    if (editingVariant) {
                      await updateVariant(accessToken, editingVariant.id, body);
                      setEditingVariant(null);
                    } else {
                      await createVariant(accessToken, productId, body);
                    }
                  },
                  editingVariant ? 'Вариант обновлён' : 'Вариант создан',
                  form,
                );
              }}
            >
              <label>
                Продукт
                <select
                  name="productId"
                  required
                  value={variantProductId}
                  disabled={Boolean(editingVariant)}
                  onChange={(event) => setVariantProductId(event.target.value)}
                >
                  <option value="">Выберите продукт</option>
                  {products.map((product) => (
                    <option key={product.id} value={product.id}>
                      {product.name}
                    </option>
                  ))}
                </select>
              </label>
              <div className="form-row">
                <label>
                  SKU
                  <input name="sku" defaultValue={editingVariant?.sku ?? ''} required />
                </label>
                <label>
                  Название
                  <input name="name" defaultValue={editingVariant?.name ?? ''} required />
                </label>
              </div>
              <div className="form-row">
                <label>
                  Размер
                  <input name="size" defaultValue={editingVariant?.size ?? ''} />
                </label>
                <label>
                  Горшок
                  <input name="potSize" defaultValue={editingVariant?.potSize ?? ''} />
                </label>
                <label>
                  Форма
                  <input name="form" defaultValue={editingVariant?.form ?? ''} />
                </label>
              </div>
              <div className="form-row">
                <label>
                  Цена, BYN
                  <input
                    name="price"
                    type="number"
                    min="0"
                    step="0.01"
                    defaultValue={
                      editingVariant ? (editingVariant.priceMinor / 100).toString() : ''
                    }
                    required
                  />
                </label>
                <label>
                  Статус
                  <select name="status" defaultValue={editingVariant?.status ?? 'ACTIVE'}>
                    <option>DRAFT</option>
                    <option>ACTIVE</option>
                    <option>ARCHIVED</option>
                  </select>
                </label>
              </div>
              <button type="submit">
                {editingVariant ? 'Сохранить вариант' : 'Создать вариант'}
              </button>
              {editingVariant ? (
                <button type="button" className="secondary" onClick={() => setEditingVariant(null)}>
                  Отмена
                </button>
              ) : null}
            </form>
          </div>
          <div className="card panel">
            <h2>Варианты продукта</h2>
            <table>
              <thead>
                <tr>
                  <th>SKU</th>
                  <th>Название</th>
                  <th>Цена</th>
                  <th>Статус</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {variants.map((variant) => (
                  <tr key={variant.id}>
                    <td className="mono">{variant.sku}</td>
                    <td>{variant.name}</td>
                    <td>{formatPrice(variant.priceMinor, variant.currency)}</td>
                    <td>{variant.status}</td>
                    <td>
                      <button
                        type="button"
                        onClick={() => {
                          setEditingVariant(variant);
                          setVariantProductId(variant.productId);
                        }}
                      >
                        Редактировать
                      </button>
                      {variant.status !== 'ARCHIVED' ? (
                        <button
                          type="button"
                          onClick={() =>
                            void run(
                              () => archiveVariant(accessToken, variant.id),
                              'Вариант архивирован',
                            )
                          }
                        >
                          Архивировать
                        </button>
                      ) : null}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      ) : null}

      {tab === 'stock' ? (
        <section className="panel-grid">
          <div className="card panel">
            <h2>Новая партия</h2>
            <form
              onSubmit={(event) => {
                event.preventDefault();
                const form = event.currentTarget;
                const data = new FormData(form);
                void run(
                  () =>
                    createStockBatch(accessToken, {
                      productVariantId: formValue(data, 'productVariantId'),
                      supplier: formValue(data, 'supplier'),
                      quantity: formNumber(data, 'quantity'),
                      unitCostMinor: Math.round(formNumber(data, 'unitCost') * 100),
                      currency: CURRENCY,
                      quarantineUntil: new Date(formValue(data, 'quarantineUntil')).toISOString(),
                    }),
                  'Партия создана (QUARANTINE)',
                  form,
                );
              }}
            >
              <label>
                Продукт
                <select
                  value={stockProductId}
                  onChange={(event) => setStockProductId(event.target.value)}
                >
                  <option value="">Выберите продукт</option>
                  {products.map((product) => (
                    <option key={product.id} value={product.id}>
                      {product.name}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Вариант
                <select name="productVariantId" required>
                  {stockVariants.map((variant) => (
                    <option key={variant.id} value={variant.id}>
                      {variant.sku} · {variant.name}
                    </option>
                  ))}
                </select>
              </label>
              <div className="form-row">
                <label>
                  Поставщик
                  <input name="supplier" required />
                </label>
                <label>
                  Количество
                  <input name="quantity" type="number" min="1" required />
                </label>
              </div>
              <div className="form-row">
                <label>
                  Себестоимость, BYN
                  <input name="unitCost" type="number" min="0" step="0.01" required />
                </label>
                <label>
                  Карантин до
                  <input
                    name="quarantineUntil"
                    type="datetime-local"
                    defaultValue="2030-01-01T00:00"
                    required
                  />
                </label>
              </div>
              <button type="submit">Создать партию</button>
            </form>
          </div>
          <div className="card panel">
            <h2>Партии</h2>
            <table>
              <thead>
                <tr>
                  <th>Вариант</th>
                  <th>Поставщик</th>
                  <th>Остаток</th>
                  <th>Статус</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {batches.map((batch) => (
                  <tr key={batch.id}>
                    <td className="mono">{batch.productVariantId.slice(0, 8)}</td>
                    <td>{batch.supplier}</td>
                    <td>
                      {batch.remainingQuantity}/{batch.quantity}
                    </td>
                    <td>{batch.status}</td>
                    <td>
                      {batch.status === 'QUARANTINE' ? (
                        <button
                          type="button"
                          onClick={() =>
                            void run(
                              () => releaseStockBatch(accessToken, batch.id),
                              'Партия опубликована',
                            )
                          }
                        >
                          Выпустить
                        </button>
                      ) : null}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      ) : null}
    </main>
  );
}
