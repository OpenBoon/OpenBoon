export const DEFAULT_MIN = 0
export const DEFAULT_MAX = 1

export const INITIAL_STATE = {
  width: 0,
  height: 0,
  labelsWidth: 100,
  zoom: 1,
  isMinimapOpen: true,
  isPreviewOpen: false,
  selectedCell: [],
  isNormalized: true,
  minScore: DEFAULT_MIN,
  maxScore: DEFAULT_MAX,
}

export const reducer = (state, action) => ({ ...state, ...action })
