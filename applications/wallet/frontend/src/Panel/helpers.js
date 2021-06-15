import { useLocalStorage } from '../LocalStorage/helpers'

import { ACTIONS, reducer } from '../Resizeable/reducer'

export { ACTIONS }

export const MIN_WIDTH = 400

export const usePanel = ({ openToThe }) => {
  return useLocalStorage({
    key: `${openToThe}OpeningPanelSettings`,
    reducer,
    initialState: {
      size: MIN_WIDTH,
      originSize: 0,
      isOpen: false,
      openPanel: '',
    },
  })
}

export const onMouseUp = ({ minWidth }) => {
  return ({ newSize }) => {
    if (newSize < minWidth) {
      return {
        openPanel: '',
      }
    }

    return {}
  }
}
