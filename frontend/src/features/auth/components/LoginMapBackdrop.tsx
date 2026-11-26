export function LoginMapBackdrop() {
  return (
    <div className="login-map-backdrop" aria-hidden="true">
      <svg viewBox="0 0 1600 1000" preserveAspectRatio="xMidYMid slice">
        <g className="login-map-contours">
          <path d="M-100 240C110 80 260 360 450 190S790 90 970 240 1320 340 1700 100" />
          <path d="M-140 300C80 140 280 420 470 250S810 150 990 300 1340 400 1740 160" />
          <path d="M-180 360C50 200 300 480 490 310S830 210 1010 360 1360 460 1780 220" />
          <path d="M-200 700C30 540 240 820 440 640S780 550 970 700 1320 800 1740 570" />
          <path d="M-240 760C-10 600 260 880 460 700S800 610 990 760 1340 860 1780 630" />
        </g>
        <g className="login-map-fields">
          <path d="m85 425 145-75 102 58-38 132-161 10z" />
          <path d="m332 408 126-34 74 85-54 119-184-38z" />
          <path d="m1180 285 210 0 60 105-210 40z" />
          <path d="m1150 705 210-25 70 110-220 60z" />
        </g>
        <path
          className="login-map-route"
          d="M-40 560C200 420 320 690 540 520S900 400 1090 570 1390 700 1640 480"
        />
      </svg>
    </div>
  )
}
