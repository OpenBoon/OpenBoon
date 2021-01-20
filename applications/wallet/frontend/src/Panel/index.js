import PropTypes from 'prop-types'

import {useLocalStorage} from '../LocalStorage/helpers'

import {reducer} from '../Resizeable/reducer'

import Resizeable from '../Resizeable'

import PanelHeader from './Header'
import PanelContent from './Content'

import {onMouseUp} from './helpers'

export const MIN_WIDTH = 400

const Panel = ({ openToThe, children }) => {
  const [{ openPanel, isOpen }, dispatch] = useLocalStorage({
    key: `${openToThe}OpeningPanelSettings`,
    reducer,
    initialState: {
      size: MIN_WIDTH,
      originSize: 0,
      isOpen: false,
      openPanel: '',
    },
  })

  const panel = children[openPanel] || {}

  return (
    <Resizeable
      storageName={`${openToThe}OpeningPanelSettings`}
      minSize={MIN_WIDTH}
      openToThe={openToThe}
      isInitiallyOpen={false}
      isDisabled={!panel.title}
      onMouseUp={onMouseUp({ minWidth: MIN_WIDTH })}
      header={
        <PanelHeader
          openPanel={openPanel}
          dispatch={dispatch}
          minWidth={MIN_WIDTH}
        >
          {children}
        </PanelHeader>
      }
    >
      {isOpen && (
        <PanelContent openToThe={openToThe} panel={panel} dispatch={dispatch} />
      )}
    </Resizeable>
  )
}

Panel.propTypes = {
  openToThe: PropTypes.oneOf(['left', 'right']).isRequired,
  children: PropTypes.objectOf(
    PropTypes.shape({
      title: PropTypes.string.isRequired,
      icon: PropTypes.node.isRequired,
      content: PropTypes.node.isRequired,
      isBeta: PropTypes.bool,
    }).isRequired,
  ).isRequired,
}

export default Panel
