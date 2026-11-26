import { useState } from 'react'

interface LoginPasswordInputProps {
  autoComplete: string
  id: string
  name: string
  onChange: (value: string) => void
  value: string
}

export function LoginPasswordInput({
  autoComplete,
  id,
  name,
  onChange,
  value,
}: LoginPasswordInputProps) {
  const [isVisible, setIsVisible] = useState(false)

  return (
    <span className="password-input-wrap">
      <input
        autoComplete={autoComplete}
        id={id}
        name={name}
        required
        type={isVisible ? 'text' : 'password'}
        value={value}
        onChange={(event) => onChange(event.currentTarget.value)}
      />
      <button
        aria-label={isVisible ? 'Hide password' : 'Show password'}
        aria-pressed={isVisible}
        className="password-visibility-toggle"
        type="button"
        onClick={() => setIsVisible(!isVisible)}
      >
        <svg
          aria-hidden="true"
          className={`password-eye-icon${isVisible ? ' password-eye-icon-visible' : ''}`}
          fill="none"
          viewBox="0 0 24 24"
        >
          <path
            className="password-eye-outline"
            d="M2.5 12s3.2-6 9.5-6 9.5 6 9.5 6-3.2 6-9.5 6-9.5-6-9.5-6Z"
          />
          <circle className="password-eye-pupil" cx="12" cy="12" r="2.7" />
          <path className="password-eye-slash" d="m4 4 16 16" />
        </svg>
      </button>
    </span>
  )
}
