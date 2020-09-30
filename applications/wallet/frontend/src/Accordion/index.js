import PropTypes from 'prop-types'

import { useLocalStorage } from '../LocalStorage/helpers'

import { typography, colors, spacing, constants } from '../Styles'

import ChevronSvg from '../Icons/chevron.svg'

const STYLES = {
  PRIMARY: {
    PADDING: spacing.normal,
    BACKGROUND_COLOR: colors.structure.smoke,
    SUMMARY_BACKGROUND_COLOR: colors.structure.smoke,
    details: {},
    title: {
      fontSize: typography.size.medium,
      lineHeight: typography.height.medium,
      fontWeight: typography.weight.bold,
    },
    content: {
      borderTop: constants.borders.regular.iron,
      padding: spacing.spacious,
      paddingTop: spacing.normal,
    },
  },
  PANEL: {
    PADDING: spacing.moderate,
    BACKGROUND_COLOR: colors.structure.coal,
    SUMMARY_BACKGROUND_COLOR: colors.structure.lead,
    details: {
      borderBottom: constants.borders.regular.smoke,
    },
    title: {},
    content: {},
  },
  FILTER: {
    PADDING: spacing.base,
    BACKGROUND_COLOR: colors.structure.coal,
    SUMMARY_BACKGROUND_COLOR: colors.structure.lead,
    details: {
      borderBottom: constants.borders.regular.smoke,
    },
    title: {
      fontFamily: typography.family.mono,
      fontSize: typography.size.small,
      lineHeight: typography.height.small,
      paddingRight: spacing.small,
    },
    content: {},
  },
}

export const VARIANTS = Object.keys(STYLES).reduce(
  (accumulator, style) => ({ ...accumulator, [style]: style }),
  {},
)

const Accordion = ({
  variant,
  icon,
  title,
  hideTitle,
  actions,
  cacheKey,
  children,
  isInitiallyOpen,
  isResizeable,
}) => {
  const [isOpen, setOpen] = useLocalStorage({
    key: cacheKey,
    initialState: isInitiallyOpen,
  })

  return (
    <details
      css={{
        ':hover': { summary: { svg: { opacity: 1 } } },
        borderRadius: constants.borderRadius.small,
        backgroundColor: STYLES[variant].BACKGROUND_COLOR,
        ...STYLES[variant].details,
      }}
      open={isOpen}
      onToggle={({ target: { open } }) => setOpen({ value: open })}
    >
      <summary
        aria-label={title}
        css={{
          listStyleType: 'none',
          '::-webkit-details-marker': { display: 'none' },
          ':hover': {
            cursor: 'pointer',
            backgroundColor: colors.structure.mattGrey,
          },
          padding: STYLES[variant].PADDING,
          backgroundColor: STYLES[variant].SUMMARY_BACKGROUND_COLOR,
        }}
      >
        <div css={{ display: 'flex', alignItems: 'center' }}>
          <ChevronSvg
            height={constants.icons.regular}
            css={{
              color: colors.structure.steel,
              transform: isOpen ? 'rotate(-180deg)' : '',
            }}
          />

          {!!icon && (
            <span
              css={{
                display: 'flex',
                paddingLeft: STYLES[variant].PADDING,
              }}
            >
              {icon}
            </span>
          )}

          {!hideTitle && (
            <span
              css={{
                flex: 1,
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                whiteSpace: 'nowrap',
                marginTop: -spacing.mini,
                paddingLeft: STYLES[variant].PADDING,
                ...STYLES[variant].title,
              }}
            >
              {title}
            </span>
          )}

          {actions}
        </div>
      </summary>

      <div
        css={{
          ...STYLES[variant].content,
          ...(isResizeable ? { maxHeight: 500, overflowY: 'auto' } : {}),
        }}
      >
        {children}
      </div>
    </details>
  )
}

Accordion.defaultProps = {
  icon: false,
  hideTitle: false,
  actions: false,
}

Accordion.propTypes = {
  variant: PropTypes.oneOf(Object.keys(VARIANTS)).isRequired,
  icon: PropTypes.node,
  title: PropTypes.string.isRequired,
  hideTitle: PropTypes.bool,
  actions: PropTypes.node,
  cacheKey: PropTypes.string.isRequired,
  children: PropTypes.node.isRequired,
  isInitiallyOpen: PropTypes.bool.isRequired,
  isResizeable: PropTypes.bool.isRequired,
}

export default Accordion
