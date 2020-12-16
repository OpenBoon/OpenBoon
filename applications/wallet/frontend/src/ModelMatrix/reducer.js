export const INITIAL_STATE = {
  width: 0,
  height: 0,
  labelsWidth: 100,
  zoom: 1,
  isMinimapOpen: true,
  isPreviewOpen: false,
  isNormalized: true,
}

export const reducer = (state, action) => ({ ...state, ...action })
