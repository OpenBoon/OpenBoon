/* eslint-disable jsx-a11y/no-static-element-interactions */
import PropTypes from 'prop-types'

import { useLocalStorage } from '../LocalStorage/helpers'

import ResizeableDropMessage from './DropMessage'
import ResizeableHandle from './Handle'

import { reducer } from './reducer'
import { getToggleOpen, getHandleMouseMove, getHandleMouseUp } from './helpers'

export const noop = () => {}

const Resizeable = ({
  storageName,
  minSize,
  openToThe,
  isInitiallyOpen,
  onMouseUp,
  isDisabled,
  header,
  children,
}) => {
  const [state, dispatch] = useLocalStorage({
    key: storageName,
    reducer,
    initialState: {
      size: minSize,
      originSize: isInitiallyOpen ? minSize : 0,
      isOpen: isInitiallyOpen,
    },
  })

  /* istanbul ignore next */
  const { size, originSize, isOpen } = state || {}

  const isHorizontal = openToThe === 'left' || openToThe === 'right'

  const toggleOpen = getToggleOpen({ dispatch, minSize })
  const handleMouseMove = getHandleMouseMove({ isOpen, size, dispatch })
  const handleMouseUp = getHandleMouseUp({
    isOpen,
    size,
    minSize,
    onMouseUp,
    dispatch,
  })

  return (
    <div
      style={{
        display: 'flex',
        [isHorizontal ? 'height' : 'width']: '100%',
        flexDirection: isHorizontal ? 'row' : 'column',
        alignItems: 'stretch',
        flexWrap: 'nowrap',
      }}
    >
      {(openToThe === 'left' || openToThe === 'top') && !isDisabled && (
        <ResizeableHandle
          openToThe={openToThe}
          onMouseMove={handleMouseMove}
          onMouseUp={handleMouseUp}
        />
      )}

      {(openToThe === 'right' || openToThe === 'top') &&
        (typeof header === 'function'
          ? header({ isOpen, toggleOpen })
          : header)}

      {size >= minSize && isOpen && (
        <div
          css={{
            [isHorizontal ? 'width' : 'height']: size,
            ...(isHorizontal
              ? {
                  display: 'flex',
                  flexDirection: 'column',
                }
              : {}),
          }}
        >
          {typeof children === 'function' ? children({ size }) : children}
        </div>
      )}

      {size < minSize && (
        /* istanbul ignore next */
        <ResizeableDropMessage
          size={size}
          originSize={originSize}
          isHorizontal={isHorizontal}
        />
      )}

      {(openToThe === 'right' || openToThe === 'bottom') && !isDisabled && (
        <ResizeableHandle
          openToThe={openToThe}
          onMouseMove={handleMouseMove}
          onMouseUp={handleMouseUp}
        />
      )}

      {(openToThe === 'left' || openToThe === 'bottom') &&
        (typeof header === 'function'
          ? header({ isOpen, toggleOpen })
          : header)}
    </div>
  )
}

Resizeable.defaultProps = {
  onMouseUp: noop,
  isDisabled: false,
  header: noop,
  children: noop,
}

Resizeable.propTypes = {
  storageName: PropTypes.string.isRequired,
  minSize: PropTypes.number.isRequired,
  openToThe: PropTypes.oneOf(['top', 'left', 'bottom', 'right']).isRequired,
  isInitiallyOpen: PropTypes.bool.isRequired,
  onMouseUp: PropTypes.func,
  isDisabled: PropTypes.bool,
  header: PropTypes.oneOfType([PropTypes.func, PropTypes.node]),
  children: PropTypes.oneOfType([PropTypes.func, PropTypes.node]),
}

export default Resizeable
