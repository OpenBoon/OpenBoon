const ToggleTipContent = ({ children }) => {
  return (
    <div
      role="tooltip"
      id="trainingHelpText"
      css={{
        position: 'absolute',
        [openToThe === 'left' ? 'right' : 'left']: -TEXTBOX_POSITION,
        top: ICON_SIZE + spacing.base,
        color: colors.structure.coal,
        backgroundColor: colors.structure.white,
        borderRadius: constants.borderRadius.small,
        padding: spacing.moderate,
        width: 'max-content',
        maxWidth: constants.toggleTip.maxWidth,
        visibility: 'hidden',
        opacity: 0,
        transition: 'all 0.5s ease 0.25s',
        ':hover': {
          visibility: 'visible',
          opacity: 1,
        },
      }}
    >
      <div
        css={{
          '&:before': {
            content: `' '`,
            position: 'absolute',
            top: -CARET_SIZE,
            [openToThe === 'left' ? 'right' : 'left']: CARET_POSITION,
            borderBottom: `${CARET_SIZE}px solid ${colors.structure.white}`,
            borderLeft: `${CARET_SIZE}px solid transparent`,
            borderRight: `${CARET_SIZE}px solid transparent`,
          },
        }}
      />
      {children}
    </div>
  )
}
