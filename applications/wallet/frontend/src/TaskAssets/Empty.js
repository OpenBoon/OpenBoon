import { typography, colors, constants } from '../Styles'

import NoJobsSvg from '../Icons/noJobs.svg'

const TaskAssetsEmpty = () => {
  return (
    <div
      css={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        height: '100%',
        fontSize: typography.size.large,
        lineHeight: typography.height.large,
        backgroundColor: colors.structure.lead,
        boxShadow: constants.boxShadows.default,
      }}
    >
      <NoJobsSvg width={200} />
      <div>This task has no assets.</div>
      <div>Any new asset will appear here.</div>
    </div>
  )
}

export default TaskAssetsEmpty
