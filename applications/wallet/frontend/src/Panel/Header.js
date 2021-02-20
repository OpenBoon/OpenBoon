import PropTypes from 'prop-types'

import { colors, spacing, constants } from '../Styles'

import { ACTIONS } from '../Resizeable/reducer'

import Button, { VARIANTS } from '../Button'
import Feature, { ENVS } from '../Feature'

const PanelHeader = ({ openPanel, dispatch, minWidth, children }) => {
  return (
    <div
      css={{
        display: 'flex',
        flexDirection: 'column',
      }}
    >
      {Object.entries(children).map(
        ([
          key,
          { title, icon, flag = '', envs = [...Object.values(ENVS)] },
        ]) => (
          <Feature key={title} flag={flag} envs={envs}>
            <Button
              aria-label={title}
              title={title}
              variant={VARIANTS.ICON}
              onClick={() => {
                if (!openPanel) {
                  dispatch({
                    type: ACTIONS.OPEN,
                    payload: {
                      openPanel: key,
                      minSize: minWidth,
                    },
                  })
                }

                if (openPanel && key === openPanel) {
                  dispatch({
                    type: ACTIONS.CLOSE,
                    payload: {
                      openPanel: '',
                    },
                  })
                }

                if (openPanel && key !== openPanel) {
                  dispatch({
                    type: ACTIONS.UPDATE,
                    payload: { openPanel: key },
                  })
                }
              }}
              style={{
                flex: 'none',
                paddingTop: spacing.normal,
                paddingBottom: spacing.normal,
                backgroundColor: colors.structure.lead,
                marginBottom: spacing.hairline,
                color:
                  key === openPanel ? colors.key.two : colors.structure.steel,
                ':hover': {
                  backgroundColor: colors.structure.mattGrey,
                },
                borderRadius: constants.borderRadius.none,
              }}
            >
              {icon}
            </Button>
          </Feature>
        ),
      )}
      <div
        css={{
          flex: 1,
          backgroundColor: colors.structure.lead,
        }}
      />
    </div>
  )
}

PanelHeader.propTypes = {
  openPanel: PropTypes.string.isRequired,
  dispatch: PropTypes.func.isRequired,
  minWidth: PropTypes.number.isRequired,
  children: PropTypes.objectOf(
    PropTypes.shape({
      title: PropTypes.string.isRequired,
      icon: PropTypes.node.isRequired,
      content: PropTypes.node.isRequired,
      isBeta: PropTypes.bool,
    }).isRequired,
  ).isRequired,
}

export default PanelHeader
