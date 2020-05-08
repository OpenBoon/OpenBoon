import { keyframes } from '@emotion/core'

import { colors, typography, spacing, constants } from '../Styles'

import GeneratingSvg from '../Icons/generating.svg'

const MIN_HEIGHT = 300

const rotate = keyframes`
  from { transform:rotate(0deg); }
  to { transform:rotate(360deg); }
`

const Loading = () => {
  return (
    <div
      className="Loading"
      css={{
        minHeight: MIN_HEIGHT,
        flex: 1,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        textAlign: 'center',
        color: colors.structure.steel,
        backgroundColor: colors.structure.lead,
        fontSize: typography.size.regular,
        lineHeight: typography.height.regular,
        boxShadow: constants.boxShadows.default,
      }}
    >
      <GeneratingSvg
        height={20}
        css={{
          animation: `${rotate} 2s linear infinite`,
          marginRight: spacing.normal,
        }}
      />
      Loading...
    </div>
  )
}

export default Loading
