import { useState } from 'react';
import type { ApiError } from '../api';

/** Shows a failed call: status code, reason and the http.cat picture of that status (hidden when offline). */
export function ErrorBox({ error }: { error: ApiError | null }) {
  const [failedImage, setFailedImage] = useState<number | null>(null);
  if (!error) return null;

  return (
    <div className="status err" role="alert">
      {error.status > 0 && failedImage !== error.status && (
        <img
          className="cat"
          src={`https://http.cat/${error.status}.jpg`}
          alt={`HTTP ${error.status}`}
          onError={() => setFailedImage(error.status)}
        />
      )}
      <div>
        <strong>{error.status || 'No connection'}</strong>
        <div>{error.message}</div>
      </div>
    </div>
  );
}
