import { useState } from 'react';
import type { ChangeEvent } from 'react';
import { updatePlantPhotos, uploadMedia } from './api';
import type { IndividualPlant, PlantPhotoKind } from './api';

const KINDS: { kind: PlantPhotoKind; label: string }[] = [
  { kind: 'GENERAL', label: 'Общий план' },
  { kind: 'LEAF', label: 'Лист крупно' },
  { kind: 'ROOTS', label: 'Корни и основание' },
  { kind: 'DEFECTS', label: 'Дефекты' },
];

export function PlantGalleryEditor({
  accessToken,
  plant,
  onSaved,
}: {
  accessToken: string;
  plant: IndividualPlant;
  onSaved: (plant: IndividualPlant) => void;
}) {
  const [urls, setUrls] = useState<Record<PlantPhotoKind, string>>(() => {
    const initial = { GENERAL: '', LEAF: '', ROOTS: '', DEFECTS: '' };
    for (const photo of plant.photos ?? []) {
      initial[photo.kind] = photo.url;
    }
    return initial;
  });
  const [busy, setBusy] = useState<PlantPhotoKind | 'save' | null>(null);
  const [message, setMessage] = useState('');

  async function upload(kind: PlantPhotoKind, event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    if (!file) {
      return;
    }
    setBusy(kind);
    setMessage('');
    try {
      const url = await uploadMedia(accessToken, file);
      setUrls((previous) => ({ ...previous, [kind]: url }));
      setMessage('Фото загружено — не забудьте сохранить галерею');
    } catch (cause) {
      setMessage(cause instanceof Error ? cause.message : 'Не удалось загрузить фото');
    } finally {
      setBusy(null);
      event.target.value = '';
    }
  }

  async function save() {
    setBusy('save');
    setMessage('');
    try {
      const photos = KINDS.filter(({ kind }) => urls[kind].trim()).map(({ kind }) => ({
        kind,
        url: urls[kind].trim(),
      }));
      const updated = await updatePlantPhotos(accessToken, plant.id, photos);
      onSaved(updated);
      setMessage('Галерея сохранена');
    } catch (cause) {
      setMessage(cause instanceof Error ? cause.message : 'Не удалось сохранить галерею');
    } finally {
      setBusy(null);
    }
  }

  return (
    <div className="card panel gallery-editor">
      <h2>Галерея WYSIWYG</h2>
      <p className="muted">
        Минимум общий план. Фотостандарт: нейтральный фон, один свет, 4 ракурса.
      </p>
      {KINDS.map(({ kind, label }) => (
        <div className="gallery-editor-row" key={kind}>
          <label>
            {label}
            <input
              value={urls[kind]}
              onChange={(event) =>
                setUrls((previous) => ({ ...previous, [kind]: event.target.value }))
              }
              placeholder="https://…"
            />
          </label>
          <label className="gallery-upload">
            {busy === kind ? 'Загрузка…' : 'Загрузить'}
            <input
              type="file"
              accept="image/jpeg,image/png,image/webp"
              disabled={busy !== null}
              onChange={(event) => void upload(kind, event)}
            />
          </label>
        </div>
      ))}
      <button type="button" disabled={busy !== null} onClick={() => void save()}>
        {busy === 'save' ? 'Сохраняем…' : 'Сохранить галерею'}
      </button>
      {message ? <p className="form-message">{message}</p> : null}
    </div>
  );
}
