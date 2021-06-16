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
          Train a model without applying it to your assets.
          <h3
            css={{
              fontSize: typography.size.regular,
              lineHeight: typography.height.regular,
              paddingTop: spacing.normal,
            }}
          >
            Train &amp; Test
          </h3>
          Train and apply the model to your test set ONLY. This enables you to
          view the results in the matrix and assess your model’s performance. To
          save processing time, make adjustments to your model before applying
          it to all of your assets.
          <h3
            css={{
              fontSize: typography.size.regular,
              lineHeight: typography.height.regular,
              paddingTop: spacing.normal,
            }}
          >
            Train &amp; Apply
          </h3>
          Train and apply the model to all the assets in the project.
          <h3
            css={{
              fontSize: typography.size.regular,
              lineHeight: typography.height.regular,
              paddingTop: spacing.normal,
            }}
          >
            Delete
          </h3>
          Delete a model. This action cannot be undone.
          <br />
          <i>
            Note: Labels that you added to your assets, either manually or
            through training, remain attached.
          </i>
        </div>
      </Toggletip>
    </div>
  )
}

export default ModelTip
