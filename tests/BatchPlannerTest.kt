import com.marketplacebatch.BatchAccount
import com.marketplacebatch.BatchPlanner

fun main(){
    val planner=BatchPlanner()
    val input=listOf(
        BatchAccount("akun-03","C","pkg.c",true,position=2),
        BatchAccount("akun-01","A","pkg.a",true,position=0),
        BatchAccount("akun-02","B","pkg.b",false,position=1),
        BatchAccount("akun-04","D","pkg.d",true,position=1)
    )
    val out=planner.ordered(input).map{it.id}
    check(out==listOf("akun-01","akun-04","akun-03")){"wrong order: $out"}
    println("BatchPlannerTest PASS")
}
