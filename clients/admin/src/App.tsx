import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import {
  createCategory,
  createIndividualPlant,
  createProduct,
  createStockBatch,
  createVariant,
  fetchCategories,
  fetchIndividualPlants,
  fetchProducts,
  fetchStockBatches,
  fetchVariants,
  formatPrice,
  login,
  releaseStockBatch,
  transitionPlant,
} from './api';
import type { Category, IndividualPlant, Product, StockBatch, Variant } from './api';

const TOKEN_KEY = 'pm.admin.accessToken';
const CURRENCY = 'BYN';

type Tab = 'products' | 'categories' | 'plants' | 'variants' | 'stock';

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
          <p className="eyebrow">Plant Market · Phase 1</p>
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
          <p className="eyebrow">Plant Market · Phase 1</p>
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

      {tab === 'products' ? (
        <section className="panel-grid">
          <div className="card panel">
            <h2>Новый продукт</h2>
            <form
              onSubmit={(event) => {
                event.preventDefault();
                const form = event.currentTarget;
                const data = new FormData(form);
                void run(
                  () =>
                    createProduct(accessToken, {
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
                    }),
                  'Продукт создан',
                  form,
                );
              }}
            >
              <div className="form-row">
                <label>
                  SKU
                  <input name="sku" required />
                </label>
                <label>
                  Название
                  <input name="name" required />
                </label>
              </div>
              <div className="form-row">
                <label>
                  Slug
                  <input name="slug" required pattern="[a-z0-9-]+" />
                </label>
                <label>
                  Категория
                  <select name="categoryId" required>
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
                  <input name="price" type="number" min="0" step="0.01" required />
                </label>
                <label>
                  Размер
                  <input name="size" />
                </label>
              </div>
              <div className="form-row">
                <label>
                  Статус
                  <select name="status" defaultValue="PUBLISHED">
                    <option>DRAFT</option>
                    <option>PUBLISHED</option>
                    <option>ARCHIVED</option>
                  </select>
                </label>
                <label>
                  Свет
                  <select name="light" defaultValue="MEDIUM">
                    <option>LOW</option>
                    <option>MEDIUM</option>
                    <option>BRIGHT</option>
                    <option>DIRECT</option>
                  </select>
                </label>
                <label>
                  Уход
                  <select name="careLevel" defaultValue="EASY">
                    <option>EASY</option>
                    <option>MEDIUM</option>
                    <option>HARD</option>
                  </select>
                </label>
              </div>
              <label>
                Описание
                <textarea name="description" rows={2} />
              </label>
              <label>
                Care guide
                <textarea name="careGuide" rows={2} />
              </label>
              <div className="checks">
                <label>
                  <input type="checkbox" name="variegated" /> Вариегатный
                </label>
                <label>
                  <input type="checkbox" name="petSafe" /> Безопасен для питомцев
                </label>
              </div>
              <button type="submit">Создать продукт</button>
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
            <h2>Новая категория</h2>
            <form
              onSubmit={(event) => {
                event.preventDefault();
                const form = event.currentTarget;
                const data = new FormData(form);
                void run(
                  () =>
                    createCategory(accessToken, {
                      name: formValue(data, 'name'),
                      slug: formValue(data, 'slug'),
                      description: formValue(data, 'description') || null,
                    }),
                  'Категория создана',
                  form,
                );
              }}
            >
              <label>
                Название
                <input name="name" required />
              </label>
              <label>
                Slug
                <input name="slug" required pattern="[a-z0-9-]+" />
              </label>
              <label>
                Описание
                <textarea name="description" rows={2} />
              </label>
              <button type="submit">Создать</button>
            </form>
          </div>
          <div className="card panel">
            <h2>Категории</h2>
            <table>
              <thead>
                <tr>
                  <th>Slug</th>
                  <th>Название</th>
                </tr>
              </thead>
              <tbody>
                {categories.map((category) => (
                  <tr key={category.id}>
                    <td className="mono">{category.slug}</td>
                    <td>{category.name}</td>
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
            <h2>Новый экземпляр</h2>
            <form
              onSubmit={(event) => {
                event.preventDefault();
                const form = event.currentTarget;
                const data = new FormData(form);
                const productId = formValue(data, 'productId');
                void run(
                  () =>
                    createIndividualPlant(accessToken, productId, {
                      code: formValue(data, 'code'),
                      description: formValue(data, 'description') || null,
                      photoUrl: formValue(data, 'photoUrl') || null,
                      priceMinor: Math.round(formNumber(data, 'price') * 100),
                      currency: CURRENCY,
                      careLevel: formValue(data, 'careLevel'),
                      light: formValue(data, 'light'),
                      petSafe: data.get('petSafe') === 'on',
                    }),
                  'Экземпляр создан (ACQUIRED)',
                  form,
                );
              }}
            >
              <label>
                Продукт
                <select name="productId" required>
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
                  <input name="code" required />
                </label>
                <label>
                  Цена, BYN
                  <input name="price" type="number" min="0" step="0.01" required />
                </label>
              </div>
              <div className="form-row">
                <label>
                  Свет
                  <select name="light" defaultValue="MEDIUM">
                    <option>LOW</option>
                    <option>MEDIUM</option>
                    <option>BRIGHT</option>
                    <option>DIRECT</option>
                  </select>
                </label>
                <label>
                  Уход
                  <select name="careLevel" defaultValue="EASY">
                    <option>EASY</option>
                    <option>MEDIUM</option>
                    <option>HARD</option>
                  </select>
                </label>
              </div>
              <label>
                Описание
                <textarea name="description" rows={2} />
              </label>
              <label>
                Фото URL
                <input name="photoUrl" />
              </label>
              <label>
                <input type="checkbox" name="petSafe" /> Безопасен для питомцев
              </label>
              <button type="submit">Создать экземпляр</button>
            </form>
          </div>
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
            <h2>Новый вариант</h2>
            <form
              onSubmit={(event) => {
                event.preventDefault();
                const form = event.currentTarget;
                const data = new FormData(form);
                const productId = formValue(data, 'productId');
                void run(
                  () =>
                    createVariant(accessToken, productId, {
                      sku: formValue(data, 'sku'),
                      name: formValue(data, 'name'),
                      size: formValue(data, 'size') || null,
                      potSize: formValue(data, 'potSize') || null,
                      form: formValue(data, 'form') || null,
                      priceMinor: Math.round(formNumber(data, 'price') * 100),
                      currency: CURRENCY,
                      status: formValue(data, 'status'),
                    }),
                  'Вариант создан',
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
                  <input name="sku" required />
                </label>
                <label>
                  Название
                  <input name="name" required />
                </label>
              </div>
              <div className="form-row">
                <label>
                  Размер
                  <input name="size" />
                </label>
                <label>
                  Горшок
                  <input name="potSize" />
                </label>
                <label>
                  Форма
                  <input name="form" />
                </label>
              </div>
              <div className="form-row">
                <label>
                  Цена, BYN
                  <input name="price" type="number" min="0" step="0.01" required />
                </label>
                <label>
                  Статус
                  <select name="status" defaultValue="ACTIVE">
                    <option>DRAFT</option>
                    <option>ACTIVE</option>
                    <option>ARCHIVED</option>
                  </select>
                </label>
              </div>
              <button type="submit">Создать вариант</button>
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
                </tr>
              </thead>
              <tbody>
                {variants.map((variant) => (
                  <tr key={variant.id}>
                    <td className="mono">{variant.sku}</td>
                    <td>{variant.name}</td>
                    <td>{formatPrice(variant.priceMinor, variant.currency)}</td>
                    <td>{variant.status}</td>
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
