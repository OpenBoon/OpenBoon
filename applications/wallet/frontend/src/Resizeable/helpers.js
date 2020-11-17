import { ACTIONS } from './reducer'

export const getToggleOpen = ({ dispatch, minSize }) => () =>
  dispatch({
    type: ACTIONS.TOGGLE_OPEN,
    payload: { minSize },
  })

export const getHandleMouseMove = ({ isOpen, size, dispatch }) => ({
  difference,
}) => {
  const newSize = (isOpen ? size : 0) - difference

  dispatch({ type: ACTIONS.HANDLE_MOUSE_MOVE, payload: { newSize } })
}

export const getHandleMouseUp = ({
  isOpen,
  size,
  minSize,
  onMouseUp,
  dispatch,
}) => ({ difference }) => {
  const newSize = (isOpen ? size : 0) - difference

  dispatch({
    type: ACTIONS.HANDLE_MOUSE_UP,
    payload: { newSize, minSize, ...onMouseUp({ newSize }) },
  })
}
