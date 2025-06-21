package cn.hutool.core.lang;

/**
 * 关系映射器
 *
 * @author ZhangJinyu
 * @since 2025-06-20
 */
public final class CorrelationMapping {
	private static final float DEFAULT_LOAD_FACTOR = 0.75f;
	private final List<CorrelationWrapper> dataList = new LinkedList<>();
	private Map<Class<?>, List<CorrelationWrapper>> correlationMap;
	private Map<Class<?>, List<Object>> dataMap;

	private int calculateInitialCapacity(int size) {
		return Math.max((int) (size / DEFAULT_LOAD_FACTOR) + 1, 16);
	}

	public <T, D> void bindData(T data, D obj) {
		if (Objects.isNull(data)) {
			return;
		}
		if (Objects.nonNull(obj)) {
			correlationMap = null;
			dataMap = null;
			this.dataList.add(new CorrelationWrapper(data, obj));
		}
	}

	public void completeBind() {
		if (Objects.nonNull(correlationMap)) {
			return;
		}
		correlationMap = this.dataList.stream().distinct().collect(Collectors.groupingBy(v -> v.data.getClass()));
		dataMap = new HashMap<>(calculateInitialCapacity(this.dataList.size() * 2));
		Map<Object, Long> dataCountMap = this.dataList.stream().flatMap((Function<CorrelationWrapper, Stream<Class<?>>>) v -> Stream.of(v.data.getClass(), v.bindData.getClass())).collect(Collectors.groupingBy(clazz -> clazz, Collectors.counting()));
		for (CorrelationWrapper v : this.dataList) {
			Class<?> dataClass = v.data.getClass();
			Class<?> bindDataClass = v.bindData.getClass();
			dataMap.computeIfAbsent(dataClass, clazz -> new ArrayList<>(Math.toIntExact(dataCountMap.get(dataClass)))).add(v.data);
			dataMap.computeIfAbsent(bindDataClass, clazz -> new ArrayList<>(Math.toIntExact(dataCountMap.get(bindDataClass)))).add(v.bindData);
		}
	}

	@SuppressWarnings("unchecked")
	public <T, D> void mapping(Class<T> dataClass, Class<D> bindDataClass, Mapping<T, D> mapping) {
		if (Objects.isNull(correlationMap) || Objects.isNull(dataClass) || Objects.isNull(bindDataClass) || Objects.isNull(mapping)) {
			return;
		}
		List<CorrelationWrapper> correlationWrappers = correlationMap.get(dataClass);
		if (Objects.isNull(correlationWrappers)) {
			return;
		}
		for (CorrelationWrapper correlationWrapper : correlationWrappers) {
			if (bindDataClass.equals(correlationWrapper.bindData.getClass())) {
				mapping.mapping((T) correlationWrapper.data, (D) correlationWrapper.bindData);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public <T> List<T> getData(Class<T> dataClass) {
		if (Objects.isNull(correlationMap)) {
			return new ArrayList<>();
		}
		return (List<T>) dataMap.getOrDefault(dataClass, new ArrayList<>()).stream().distinct().collect(Collectors.toList());
	}

	public interface Mapping<T, R> {
		/**
		 * 映射
		 *
		 * @param data     数据
		 * @param bindData 绑定的数据
		 */
		void mapping(T data, R bindData);
	}

	/**
	 * 关系包装器
	 *
	 * @author ZhangJinyu
	 * @since 2025-06-20
	 */
	public static class CorrelationWrapper {
		private final Object data;
		private final Object bindData;

		public CorrelationWrapper(Object data, Object bindData) {
			this.data = data;
			this.bindData = bindData;
		}

		public Object getData() {
			return data;
		}

		public Object getBindData() {
			return bindData;
		}

		@Override
		public final boolean equals(Object object) {
			if (!(object instanceof CorrelationWrapper that)) {
				return false;
			}
			return Objects.equals(data, that.data) && Objects.equals(bindData, that.bindData);
		}

		@Override
		public int hashCode() {
			int result = Objects.hashCode(data);
			result = 31 * result + Objects.hashCode(bindData);
			return result;
		}

		@Override
		public String toString() {
			return "CorrelationWrapper{" +
				"data=" + data +
				", bindData=" + bindData +
				'}';
		}
	}
}
