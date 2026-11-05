import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { fetchProducts, formatPrice, login } from './api';
import type { Product } from './api';

const TOKEN_KEY = 'pm.admin.accessToken';

export default function App() {
  const [email, setEmail] = useState('admin@plantmarket.local');
  const [password, setPassword] = useState('');
  const [accessToken, setAccessToken] = useState<string | null>(() =>
    localStorage.getItem(TOKEN_KEY),
  );
  const [products, setProducts] = useState<Product[]>([]);
  const [error, setError] = useState<string | null>(null);
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
      return;
    }
    let cancelled = false;
    setLoading(true);
    setError(null);
    fetchProducts(accessToken)
      .then((loaded) => {
        if (!cancelled) {
          setProducts(loaded);
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

  async function reload() {
    if (!accessToken) {
      return;
    }
    setLoading(true);
    setError(null);
    try {
      setProducts(await fetchProducts(accessToken));
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Неизвестная ошибка');
    } finally {
      setLoading(false);
    }
  }

  function logout() {
    setAccessToken(null);
    setProducts([]);
    setPassword('');
    setError(null);
  }

  if (!accessToken) {
    return (
      <main className="login-shell">
        <section className="card login-card">
          <p className="eyebrow">Plant Market · Phase 0</p>
          <h1>Админка</h1>
          <p className="muted">
            Вход через auth-service. После входа доступен read-only список продуктов.
          </p>
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
            <button type="submit" disabled={loading || loggingIn}>
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
          <p className="eyebrow">Plant Market · Phase 0</p>
          <h1>Продукты</h1>
        </div>
        <div className="actions">
          <button type="button" onClick={reload} disabled={loading}>
            Обновить
          </button>
          <button type="button" className="secondary" onClick={logout}>
            Выйти
          </button>
        </div>
      </header>

      <p className="muted">Read-only список. Всего позиций: {products.length}.</p>
      {error ? <p className="error">{error}</p> : null}

      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>SKU</th>
              <th>Название</th>
              <th>Статус</th>
              <th>Цена</th>
              <th>Свет</th>
              <th>Уход</th>
              <th>Питомцы</th>
            </tr>
          </thead>
          <tbody>
            {products.map((product) => (
              <tr key={product.id}>
                <td className="mono">{product.sku}</td>
                <td>{product.name}</td>
                <td>
                  <span className={`badge badge-${product.status.toLowerCase()}`}>
                    {product.status}
                  </span>
                </td>
                <td>{formatPrice(product.priceMinor, product.currency)}</td>
                <td>{product.light}</td>
                <td>{product.careLevel}</td>
                <td>{product.petSafe ? 'безопасно' : 'осторожно'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </main>
  );
}
