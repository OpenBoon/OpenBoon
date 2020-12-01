export const INITIAL_STATE = {
  width: 0,
  height: 0,
  zoom: 1,
  isPreviewOpen: false,
}

export const reducer = (state, action) => ({ ...state, ...action })
