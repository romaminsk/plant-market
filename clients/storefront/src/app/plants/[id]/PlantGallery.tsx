'use client';

import { useState } from 'react';
import type { PlantPhoto, PlantPhotoKind } from '@/lib/api';

const KIND_LABELS: Record<PlantPhotoKind, string> = {
  GENERAL: 'Общий план',
  LEAF: 'Лист крупно',
  ROOTS: 'Корни и основание',
  DEFECTS: 'Дефекты',
};

export function PlantGallery({
  photos,
  fallbackUrl,
  name,
  code,
}: {
  photos: PlantPhoto[];
  fallbackUrl: string | null;
  name: string;
  code: string;
}) {
  const items: PlantPhoto[] =
    photos.length > 0 ? photos : fallbackUrl ? [{ url: fallbackUrl, kind: 'GENERAL' }] : [];
  const [activeIndex, setActiveIndex] = useState(0);

  if (items.length === 0) {
    return (
      <div className="detail-photo" aria-hidden="true">
        🌿
      </div>
    );
  }

  const active = items[Math.min(activeIndex, items.length - 1)];

  return (
    <div className="plant-gallery">
      <figure className="gallery-main">
        <img
          src={active.url}
          alt={`${name}, ${KIND_LABELS[active.kind]}, код ${code}`}
          width={800}
          height={1000}
          loading="eager"
        />
        <figcaption>{KIND_LABELS[active.kind]}</figcaption>
      </figure>
      {items.length > 1 ? (
        <ul className="gallery-thumbs" aria-label="Фотографии растения">
          {items.map((photo, index) => (
            <li key={`${photo.kind}-${photo.url}`}>
              <button
                type="button"
                className={index === activeIndex ? 'gallery-thumb active' : 'gallery-thumb'}
                aria-pressed={index === activeIndex}
                onClick={() => setActiveIndex(index)}
              >
                <img
                  src={photo.url}
                  alt={`${name}: ${KIND_LABELS[photo.kind]}`}
                  width={160}
                  height={200}
                  loading="lazy"
                />
                <span>{KIND_LABELS[photo.kind]}</span>
              </button>
            </li>
          ))}
        </ul>
      ) : null}
    </div>
  );
}
