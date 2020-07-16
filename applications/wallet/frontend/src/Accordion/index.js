/* eslint-disable jsx-a11y/click-events-have-key-events */
/* eslint-disable jsx-a11y/no-static-element-interactions */
import PropTypes from 'prop-types'

import { useLocalStorageState } from '../LocalStorage/helpers'

import { typography, colors, spacing, constants } from '../Styles'

import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'

import ChevronSvg from '../Icons/chevron.svg'

const ICON_SIZE = 20

const STYLES = {
  PRIMARY: {
    container: {
      backgroundColor: colors.structure.smoke,
      borderRadius: constants.borderRadius.small,
    },
    title: {
      display: 'flex',
      borderBottom: constants.borders.regular.iron,
      paddingTop: spacing.normal,
      paddingBottom: spacing.normal,
      paddingLeft: spacing.moderate,
      ':hover': {
        cursor: 'pointer',
        backgroundColor: colors.structure.mattGrey,
      },
      h4: {
        fontWeight: typography.weight.bold,
      },
    },
    content: {
      padding: spacing.spacious,
      paddingTop: spacing.normal,
    },
  },
  PANEL: {
    container: {
      backgroundColor: colors.structure.lead,
      borderRadius: constants.borderRadius.small,
      ':last-of-type > div:last-of-type': {
        borderBottom: constants.borders.regular.smoke,
      },
      ':first-of-type > div': {
        borderTop: 'none',
      },
    },
    title: {
      display: 'flex',
      borderTop: constants.borders.regular.smoke,
      paddingTop: spacing.moderate,
      paddingBottom: spacing.moderate,
      paddingLeft: spacing.moderate,
      ':hover': {
        cursor: 'pointer',
        backgroundColor: colors.structure.mattGrey,
      },
      h4: {
        fontSize: typography.size.regular,
        fontWeight: typography.weight.regular,
      },
    },
    content: {
      width: '100%',
      backgroundColor: colors.structure.coal,
    },
  },
  FILTER: {
    container: {
      backgroundColor: colors.structure.lead,
      border: constants.borders.regular.transparent,
      borderBottom: constants.borders.regular.smoke,
      paddingBottom: spacing.hairline,
      ':hover': {
        border: constants.borders.regular.smoke,
        svg: { opacity: 1 },
      },
    },
    title: {
      display: 'flex',
      padding: spacing.base,
      ':hover': {
        cursor: 'pointer',
        backgroundColor: colors.structure.mattGrey,
      },
      h4: {
        flex: 1,
        display: 'flex',
        alignItems: 'center',
        fontWeight: typography.weight.regular,
        paddingLeft: spacing.small,
      },
    },
    content: {
      width: '100%',
      backgroundColor: colors.structure.coal,
    },
  },
}

export const VARIANTS = Object.keys(STYLES).reduce(
  (accumulator, style) => ({ ...accumulator, [style]: style }),
  {},
)

const Accordion = ({
  variant,
  title,
  actions,
  cacheKey,
  children,
  isInitiallyOpen,
  isResizeable,
}) => {
  const [isOpen, setOpen] = useLocalStorageState({
    key: cacheKey,
    initialValue: isInitiallyOpen,
  })

  const toggle = () => setOpen({ value: !isOpen })

  return (
    <div css={STYLES[variant].container}>
      <div
        css={STYLES[variant].title}
        style={
          !isOpen && variant === VARIANTS.PRIMARY ? { border: 'none' } : {}
        }
        onClick={toggle}
      >
        <Button
          aria-label={`${isOpen ? 'Collapse' : 'Expand'} Section`}
          variant={BUTTON_VARIANTS.ICON}
          onClick={toggle}
          style={{ padding: 0 }}
        >
          <ChevronSvg
            height={ICON_SIZE}
            css={{
              transform: isOpen ? 'rotate(-180deg)' : '',
            }}
          />
        </Button>

        <h4
          css={{
            fontSize: typography.size.medium,
            lineHeight: typography.height.medium,
            paddingLeft: spacing.moderate,
            paddingRight: spacing.base,
            display: 'flex',
            overflow: 'hidden',
            textOverflow: 'ellipsis',
          }}
        >
          {title}
        </h4>

        {actions}
      </div>

      {isOpen && !isResizeable && (
        <div css={STYLES[variant].content}>{children}</div>
      )}

      {isOpen && isResizeable && (
        <div
          css={[STYLES[variant].content, { maxHeight: 500, overflowY: 'auto' }]}
        >
          {children}
        </div>
      )}
    </div>
  )
}

Accordion.defaultProps = {
  actions: false,
}

Accordion.propTypes = {
  variant: PropTypes.oneOf(Object.keys(VARIANTS)).isRequired,
  title: PropTypes.node.isRequired,
  actions: PropTypes.node,
  cacheKey: PropTypes.string.isRequired,
  children: PropTypes.node.isRequired,
  isInitiallyOpen: PropTypes.bool.isRequired,
  isResizeable: PropTypes.bool.isRequired,
}

export default Accordion
