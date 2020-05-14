import PropTypes from 'prop-types'

import useLocalStorage from '../LocalStorage'

import { typography, colors, spacing, constants } from '../Styles'

import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'

import ChevronSvg from '../Icons/chevron.svg'

const CHEVRON_WIDTH = 20

const STYLES = {
  PRIMARY: {
    container: {
      backgroundColor: colors.structure.smoke,
      borderRadius: constants.borderRadius.small,
    },
    title: {
      borderBottom: constants.borders.tabs,
      paddingTop: spacing.normal,
      paddingBottom: spacing.normal,
      paddingLeft: spacing.moderate,
      display: 'flex',
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
        borderBottom: constants.borders.tabs,
      },
    },
    title: {
      borderTop: constants.borders.tabs,
      paddingTop: spacing.moderate,
      paddingBottom: spacing.moderate,
      paddingLeft: spacing.moderate,
      display: 'flex',
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
      borderRadius: constants.borderRadius.small,
      border: constants.borders.transparent,
      borderBottom: constants.borders.tabs,
      paddingBottom: spacing.hairline,
      ':hover': {
        border: constants.borders.tableRow,
        div: {
          svg: {
            visibility: 'visible',
          },
        },
      },
    },
    title: {
      paddingTop: spacing.moderate,
      paddingBottom: spacing.moderate,
      paddingLeft: spacing.moderate,
      display: 'flex',
      h4: {
        flex: 1,
        minWidth: 0,
        width: '100%',
        fontWeight: typography.weight.regular,
        paddingRight: spacing.moderate,
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
  cacheKey,
  children,
  isInitiallyOpen,
  isResizeable,
}) => {
  const [isOpen, setOpen] = useLocalStorage({
    key: cacheKey,
    initialValue: isInitiallyOpen,
  })

  return (
    <div css={STYLES[variant].container}>
      <div css={STYLES[variant].title}>
        <Button
          aria-label={`${isOpen ? 'Collapse' : 'Expand'} Section`}
          variant={BUTTON_VARIANTS.NEUTRAL}
          onClick={() => setOpen({ value: !isOpen })}
        >
          <ChevronSvg
            width={CHEVRON_WIDTH}
            color={colors.structure.steel}
            css={{
              transform: isOpen ? 'rotate(-180deg)' : '',
              ':hover': { color: colors.structure.white },
            }}
          />
        </Button>
        <h4
          css={{
            fontSize: typography.size.medium,
            lineHeight: typography.height.medium,
            paddingLeft: spacing.moderate,
            display: 'flex',
          }}
        >
          {title}
        </h4>
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

Accordion.propTypes = {
  variant: PropTypes.oneOf(Object.keys(VARIANTS)).isRequired,
  title: PropTypes.node.isRequired,
  cacheKey: PropTypes.string.isRequired,
  children: PropTypes.node.isRequired,
  isInitiallyOpen: PropTypes.bool.isRequired,
  isResizeable: PropTypes.bool.isRequired,
}

export default Accordion
