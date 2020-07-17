import PropTypes from 'prop-types'

import { useLocalStorageState } from '../LocalStorage/helpers'

import { colors, spacing, constants } from '../Styles'

import Button, { VARIANTS } from '../Button'
import Feature, { ENVS } from '../Feature'

import PanelContent from './Content'

const Panel = ({ openToThe, children }) => {
  const [openPanel, setOpenPanel] = useLocalStorageState({
    key: `${openToThe}OpeningPanel`,
    initialValue: '',
  })

  const panel = children[openPanel] || {}

  return (
    <div
      css={{
        display: 'flex',
        boxShadow: `${openToThe === 'left' ? '-' : ''}${
          constants.boxShadows.panel
        }`,
      }}
    >
      {!!panel.title && openToThe === 'left' && (
        <PanelContent
          openToThe={openToThe}
          panel={panel}
          setOpenPanel={setOpenPanel}
        />
      )}
      <div
        css={{
          display: 'flex',
          flexDirection: 'column',
          [openToThe === 'left'
            ? 'paddingLeft'
            : 'paddingRight']: spacing.hairline,
        }}
      >
        {Object.entries(children).map(
          ([key, { title, icon, featureProps: { flag = '', envs } = {} }]) => (
            <Feature
              key={title}
              flag={flag}
              envs={envs || [...Object.values(ENVS)]}
            >
              <Button
                aria-label={title}
                title={title}
                variant={VARIANTS.ICON}
                onClick={() =>
                  setOpenPanel({ value: key === openPanel ? '' : key })
                }
                isDisabled={false}
                style={{
                  flex: 'none',
                  paddingTop: spacing.normal,
                  paddingBottom: spacing.normal,
                  backgroundColor: colors.structure.lead,
                  marginBottom: spacing.hairline,
                  color:
                    key === openPanel ? colors.key.one : colors.structure.steel,
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
      {!!panel.title && openToThe === 'right' && (
        <PanelContent
          openToThe={openToThe}
          panel={panel}
          setOpenPanel={setOpenPanel}
        />
      )}
    </div>
  )
}

Panel.propTypes = {
  openToThe: PropTypes.oneOf(['left', 'right']).isRequired,
  children: PropTypes.objectOf(
    PropTypes.shape({
      title: PropTypes.string.isRequired,
      icon: PropTypes.node.isRequired,
      content: PropTypes.node.isRequired,
    }).isRequired,
  ).isRequired,
}

export default Panel
