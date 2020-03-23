import { typography } from '../Styles'

import NoJobsSvg from '../Icons/noJobs.svg'

const JobTasksEmpty = () => {
  return (
    <div
      css={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        fontSize: typography.size.large,
        lineHeight: typography.height.large,
      }}
    >
      <NoJobsSvg width={200} />
      <div>This job has no tasks.</div>
      <div>Any new task will appear here.</div>
    </div>
  )
}

export default JobTasksEmpty
