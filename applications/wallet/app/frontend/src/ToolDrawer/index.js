import PropTypes from 'prop-types'

import { colors, spacing, zIndex, constants, typography } from '../Styles'

import ToolDrawerOverlay from './Overlay'

import QueueSvg from './icons/queue.svg'
import KeySvg from './icons/key.svg'

const WIDTH = 240
const ICON_WIDTH = 20

const ToolDrawer = ({ isToolDrawerOpen, setToolDrawerOpen }) => {
  return (
    <div>
      <nav
        css={{
          width: WIDTH,
          position: 'fixed',
          height: `calc(100% - ${constants.navbar.height}px)`,
          overflowY: 'auto',
          zIndex: zIndex.layout.drawer,
          backgroundColor: colors.grey5,
          paddingBottom: spacing.spacious,
          transition: 'left ease-in-out .3s, visibility ease-in-out .3s',
          overscrollBehavior: 'contain',
          visibility: isToolDrawerOpen ? 'visible' : 'hidden',
          left: isToolDrawerOpen ? 0 : -WIDTH,
          top: constants.navbar.height,
          paddingTop: spacing.moderate,
        }}>
        <ul
          css={{
            listStyleType: 'none',
            padding: 0,
            margin: 0,
            a: {
              display: 'flex',
              alignItems: 'center',
              padding: spacing.moderate,
              fontSize: typography.size.kilo,
              color: colors.grey2,
              svg: {
                marginRight: spacing.moderate,
              },
              ':hover': {
                textDecoration: 'none',
                color: colors.plants.clover,
                backgroundColor: colors.grey1,
              },
            },
          }}>
          <li>
            <a href="/">
              <QueueSvg width={ICON_WIDTH} />
              Data Queue
            </a>
          </li>
          <li>
            <a href="/">
              <KeySvg width={ICON_WIDTH} />
              API Key
            </a>
          </li>
        </ul>
      </nav>
      <ToolDrawerOverlay
        isToolDrawerOpen={isToolDrawerOpen}
        setToolDrawerOpen={setToolDrawerOpen}
      />
    </div>
  )
}

ToolDrawer.propTypes = {
  isToolDrawerOpen: PropTypes.bool.isRequired,
  setToolDrawerOpen: PropTypes.func.isRequired,
}

export default ToolDrawer
