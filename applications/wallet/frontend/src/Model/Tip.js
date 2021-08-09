import { spacing, typography } from '../Styles'

import Toggletip from '../Toggletip'

const ModelTip = () => {
  return (
    <div
      css={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        div: { marginLeft: 0 },
        button: { marginRight: 0 },
      }}
    >
      <Toggletip openToThe="right" label="Training Options">
        <div
          css={{
            fontSize: typography.size.regular,
            lineHeight: typography.height.regular,
            padding: spacing.base,
          }}
        >
          <h3
            css={{
              fontSize: typography.size.regular,
              lineHeight: typography.height.regular,
            }}
          >
            Train Model
          </h3>
          Train a model without running it on your assets.
          <h3
            css={{
              fontSize: typography.size.regular,
              lineHeight: typography.height.regular,
              paddingTop: spacing.normal,
            }}
          >
            Train &amp; Test
          </h3>
          Train and run the model on your test set ONLY. This enables you to
          view the results in the matrix and assess your model’s performance. To
          save processing time, make adjustments to your model before running it
          on all of your assets.
          <h3
            css={{
              fontSize: typography.size.regular,
              lineHeight: typography.height.regular,
              paddingTop: spacing.normal,
            }}
          >
            Train &amp; Analyze All
          </h3>
          Train the model and run the model analysis on all project assets.
        </div>
      </Toggletip>
    </div>
  )
}

export default ModelTip
